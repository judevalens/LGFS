package lgfs.client

import ChunkServiceGrpcKt
import Gfs
import Gfs.CreateFileReq
import MasterServiceGrpcKt
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import lgfs.gfs.FileMetadata
import org.apache.commons.cli.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestException
import java.security.MessageDigest
import java.util.*
import kotlin.math.ceil
import kotlin.math.min


class Client() {
	interface Command

	val env = "DEV"

	private val logger: Logger = LoggerFactory.getLogger(this::class.java)
	private val masterAddress = "127.0.0.1"
	private val testFilePath = "/home/jude/Desktop/redis-stack-server-6.2.6-v9.focal.x86_64.tar.gz"
	private var masterApiStub: MasterServiceGrpcKt.MasterServiceCoroutineStub
	private var chunkApiStubs: HashMap<Gfs.ServerAddress, ChunkServiceGrpcKt.ChunkServiceCoroutineStub> = HashMap()
	private val DATA_PORT = 9005
	private val CHUNK_SIZE = 64 * 1000
	private val tcpSockets = HashMap<Gfs.ServerAddress, Socket>()
	private val chunksMap = HashMap<Long, Gfs.Chunk>()
	private val PAYLOAD_LEN_FIELD = 4;
	private val CLIENT_ID = "dev_client"

	init {
		logger.info("GFS client has been initialized")

		val channel = ManagedChannelBuilder.forAddress(
			masterAddress, 3000
		).usePlaintext().build()
		masterApiStub = MasterServiceGrpcKt.MasterServiceCoroutineStub(channel)

		runBlocking {
			handleCommand()
		}
	}

	private suspend fun createFile(filePathStr: String) {
		val filePath = Path.of(testFilePath)
		if (Files.exists(filePath)) {
			try {
				logger.info("Sending create request to master")
				val file = java.io.File(filePath.toUri())
				val fileMetadata = FileMetadata(
					filePath.toString(), false, file.length()
				)
				val res = masterApiStub.createFile(
					CreateFileReq.newBuilder().setFileName(filePath.toString()).setFileSize(
						file.length().toInt()
					).build()
				)
				logger.info(
					"created file at {} : {}", filePath, res.isSuccessful
				)
				if (res.isSuccessful) {
					uploadFile(
						file, res.chunksList
					)
				} else {
					logger.error(
						"Failed to create file at: {}", filePath
					)
				}
			} catch (_: IllegalArgumentException) {

			} catch (_: NullPointerException) {

			} catch (exception: StatusException) {
				logger.info("Failed to create file : $filePath")
				logger.error(exception.message)
			}
		} else {
			logger.info("File at: $filePathStr doesn't exist")
		}

	}

	private suspend fun deleteFile(filePathStr: String) {
		try {
			val res = masterApiStub.deleteFile(
				Gfs.DeleteFileReq.newBuilder().setFileName(testFilePath).build()
			)
			if (res.code == Status.OK.code.value()) {
				logger.info(
					"Successfully deleted file: {}", testFilePath
				)
			} else {
				logger.info(
					"Failed to delete file: {}", testFilePath
				)
			}
		} catch (exception: StatusException) {
			logger.error(
				"Failed to delete file: {} \n {}", testFilePath, exception.message
			)
		}

	}

	private suspend fun uploadFile(file: File, chunks: List<Gfs.Chunk>) {
		val nChunk = ceil(file.length() / CHUNK_SIZE.toDouble()).toInt()
		logger.info("Upload {} chunks for file: {}", chunks.size, file.path)
		if (nChunk != chunks.size) {
			logger.error("Inconsistent number of chunks, expected {}, have {}", nChunk, chunks.size)
			return
		}
		var batch = 0
		val chunkBatch = ArrayList<Gfs.Chunk>()
		val inputStream = file.inputStream()
		var chunkBuffer = ByteArray(CHUNK_SIZE)
		val mutationIds = ArrayList<String>()
		for (i in 0 until nChunk) {
			val chunk = chunks[i]
			if (chunk.chunkIndex != i) {
				logger.error("Chunk List is out of order, expected chunk index : {}, have: {}", i, chunk.chunkIndex)
				TODO()
				break
			}
			batch++
			chunkBatch.add(chunk)
			if (batch == 5 || batch == nChunk) {
				val leaseGrantRes =
					masterApiStub.getLease(Gfs.LeaseGrantReq.newBuilder().addAllChunks(chunkBatch).build())
				logger.info("received {} leases", leaseGrantRes.leasesList.size)
				logger.info("******************sending batch of: {} ******************\n", batch)
				var k = i - batch

				sendBatch(leaseGrantRes.leasesList, file, inputStream, chunkBatch, i)

				chunkBatch.clear()
				batch = 0
			}
		}
	}

	private fun buildChunkPacket(payload: ByteArray, payloadHash: ByteArray): ByteArray {

		val payloadLen = payload.size
		val payLoadHashLen =  payloadHash.size
		val payloadLenBuff = ByteBuffer.allocate(4).putInt(payloadLen).array()
		val payloadHashBuff = ByteBuffer.allocate(4).putInt(payLoadHashLen).array()
		return payloadHashBuff + payloadHash + payloadLenBuff + payload
	}

	private suspend fun sendBatch(
		leases: List<Gfs.Lease>,
		file: File,
		inputStream: InputStream,
		chunkBatch: List<Gfs.Chunk>,
		chunkIndex: Int,
	) {
		var chunkBuffer = ByteArray(CHUNK_SIZE)

		val k = chunkIndex - chunkBatch.size

		for (i in chunkBatch.indices) {
			val lease = leases[i]
			val chunk = chunkBatch[i]

			val realChunkSize = min(CHUNK_SIZE.toLong(), file.length() - CHUNK_SIZE * (k + i)).toInt()

			if (realChunkSize < CHUNK_SIZE) {
				chunkBuffer = ByteArray(realChunkSize)
			}
			logger.info("real chunk size: {}", realChunkSize)
			val nBytes = withContext(Dispatchers.IO) {
				inputStream.readNBytes(
					chunkBuffer, 0, realChunkSize
				)
			}

			if (realChunkSize != nBytes) {
				logger.error(
					"Read incorrect amount of data, expected {}, got {}", realChunkSize, nBytes
				)
				TODO()
				return
			}

			val chunkHash = HexFormat.of().formatHex(getHash(chunkBuffer, realChunkSize))

			logger.info(
				"{} is the primary chunk server for: chunk {} with index {}",
				lease.primary.hostName,
				chunkHash.slice(0..8),
				lease.chunk.chunkIndex
			)

			val chunkServerHostnames = ArrayList(lease.replicasList + lease.primary)
			chunkServerHostnames.forEach { serverAddress ->
				logger.info("sending packet {}, to: {}", chunkHash.slice(0..8), serverAddress.hostName)
				val socket = tcpSockets.getOrPut(serverAddress) { Socket("127.0.0.1", serverAddress.dataPort) }
				try {
					val packet = buildChunkPacket(chunkBuffer, chunkHash.toByteArray())
					socket.getOutputStream().write(packet)
					logger.info("done sending chunk")
				} catch (exception: IOException) {
					logger.error(exception.message)
				}
			}

			val chunkApiStub = chunkApiStubs.getOrPut(lease.primary) { getChunkApiStub(lease.primary) }
			try {
				val res = chunkApiStub.addMutations(
					Gfs.Mutations.newBuilder().addMutations(
						Gfs.Mutation.newBuilder()
							.setClientId(CLIENT_ID)
							.setMutationId(chunkHash)
							.setChunk(chunk)
							.setType(Gfs.MutationType.Write)
							.setPrimary(lease.primary)
							.addAllReplicas(lease.replicasList)
							.setSerial(0)
							.setOffset(0)
							.build()
					).build()
				)
				logger.info("res status: {}", res.status)
			} catch (status: StatusException) {
				logger.error("Failed to send chunk mutations: {}", status.message)
			}
		}
	}


	private fun getHash(buffer: ByteArray, len: Int): ByteArray {
		val md = MessageDigest.getInstance("SHA-256")
		logger.info(
			"hash len: {}", md.digestLength
		)
		try {
			return md.digest(buffer)

		} catch (exception: DigestException) {
			logger.error(
				"failed to create hash: {}", exception.message
			)
		}
		return ByteArray(0)
	}

	private fun getChunkApiStub(serverAddress: Gfs.ServerAddress): ChunkServiceGrpcKt.ChunkServiceCoroutineStub {
		val chunkStubChannel =
			ManagedChannelBuilder.forAddress("127.0.0.1", serverAddress.apiPort).usePlaintext().build()
		return ChunkServiceGrpcKt.ChunkServiceCoroutineStub(chunkStubChannel)
	}

	private suspend fun handleCommand() {
		val parser: CommandLineParser = DefaultParser()

		val options = Options();

		val createFile =
			Option.builder("c").longOpt("create").argName("path").numberOfArgs(Option.UNLIMITED_VALUES).hasArgs()
				.desc("create a new with the specified path").build()

		val deleteFile =
			Option.builder("d").longOpt("delete").argName("path").numberOfArgs(Option.UNLIMITED_VALUES).hasArgs()
				.desc("Delete the file at the provided path").build()



		options.addOption(createFile)
		options.addOption(deleteFile)

		while (true) {
			println("enter command :")
			val commands = readln().split(" ").toTypedArray()
			try {
				val cmdLine = parser.parse(
					options, commands
				)
				if (cmdLine.hasOption("create")) {
					val filePath = cmdLine.getOptionValue("create")
					createFile(filePath)
				} else if (cmdLine.hasOption(deleteFile.longOpt)) {
					val filePath = cmdLine.getOptionValue(deleteFile.longOpt)
					deleteFile(filePath)
				} else {
					throw ParseException("Unrecognized option: ${commands[0]}")
				}
			} catch (exception: ParseException) {
				val formatter = HelpFormatter()
				println(exception.toString())
				formatter.printHelp(
					"LGFS", options
				)
			}

		}
	}
}