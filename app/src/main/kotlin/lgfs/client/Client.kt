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
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestException
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.min


class Client() {
    interface Command

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val masterAddress = "172.20.128.2"
    private val testFilePath = "/home/jude/Documents/LGFS/.gitignore"
    private var masterApiStub: MasterServiceGrpcKt.MasterServiceCoroutineStub
    private var chunkApiStubs: HashMap<String, ChunkServiceGrpcKt.ChunkServiceCoroutineStub> = HashMap()
    private val DATA_PORT = 9005
    private val CHUNK_SIZE = 64 * 1000 * 1000
    private val tcpSockets = HashMap<String, Socket>()
    private val chunksMap = HashMap<Long, Gfs.Chunk>()
    private val PAYLOAD_LEN_FIELD = 4;

    init {
        logger.info("GFS client has been initialized")

        val channel = ManagedChannelBuilder
            .forAddress(masterAddress, 7009)
            .usePlaintext()
            .build()
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
                val fileMetadata = FileMetadata(filePath.toString(), false, file.length())
                val res = masterApiStub.createFile(
                    CreateFileReq.newBuilder()
                        .setFileName(filePath.toString())
                        .setFileSize(file.length().toInt())
                        .build()
                )
                logger.info("created file at {} : {}", filePath, res.isSuccessful)
                if (res.isSuccessful) {
                    uploadFile(file, res.chunksList)
                } else {
                    logger.error("Failed to create file at: {}", filePath)
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
            val res = masterApiStub.deleteFile(Gfs.DeleteFileReq.newBuilder().setFileName(testFilePath).build())
            if (res.code == Status.OK.code.value()) {
                logger.info("Successfully deleted file: {}", testFilePath)
            } else {
                logger.info("Failed to delete file: {}", testFilePath)
            }
        } catch (exception: StatusException) {
            logger.error("Failed to delete file: {} \n {}", testFilePath, exception.message)
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
                val realChunkSize = min(CHUNK_SIZE.toLong(), file.length() - CHUNK_SIZE * i).toInt()
                if (realChunkSize < CHUNK_SIZE) {
                    chunkBuffer = ByteArray(realChunkSize)
                }
                logger.info("real chunk size: {}", realChunkSize)
                val nBytes = withContext(Dispatchers.IO) {
                    inputStream.readNBytes(chunkBuffer, 0, realChunkSize)
                }
                logger.info(
                    "real chunk size: {}, read: {}, chunk hash: {}, lease size: {}", realChunkSize, nBytes,
                    "test hash", leaseGrantRes.leasesList.size
                )
                if (realChunkSize != nBytes) {
                    logger.error("Read incorrect amount of data, expected {}, got {}", realChunkSize, nBytes)
                    TODO()
                    break
                }

                val chunkHash = getHash(chunkBuffer, realChunkSize)
                logger.info("chunk hash: {}", String(chunkHash))
                mutationIds.add(String(chunkHash))
                logger.info(
                    "real chunk size: {}, read: {}, chunk hash: {}, lease size: {}", realChunkSize, nBytes,
                    String(chunkHash), leaseGrantRes.leasesList.size
                )
                leaseGrantRes.leasesList.forEach { lease ->
                    logger.info(
                        "{} is the primary chunk server for: chunk {} with index {}", lease.primary, chunkHash,
                        lease.chunk.chunkIndex
                    )
                    val chunkServerHostnames = ArrayList(lease.replicasList + lease.primary)
                    chunkServerHostnames.forEach { hostname ->
                        logger.info("sending packet to: {}", hostname)
                        val socket = tcpSockets.getOrPut(hostname) { Socket(hostname, DATA_PORT) }
                        try {
                            val packet = buildChunkPacket(chunkBuffer, chunkHash)
                            logger.info("built packet, size: {}", packet.size)
                            logger.info("done sending chunk")
                        } catch (exception: IOException) {
                            logger.error(exception.message)
                        }
                    }

                }

                chunkBatch.clear()
                batch = 0
            }
        }
    }

    private fun buildChunkPacket(payload: ByteArray, payloadHash: ByteArray): ByteArray {

        val payloadLen = (payload.size + payloadHash.size)

        val payloadLenBuff = ByteBuffer.allocate(4).putInt(payloadLen).array()

        return payloadLenBuff + payloadHash + payload
    }

    private fun getHash(buffer: ByteArray, len: Int): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        logger.info("hash len: {}", md.digestLength)
        try {
            return md.digest(buffer)

        } catch (exception: DigestException) {
            logger.error("failed to create hash: {}", exception.message)
        }
        return ByteArray(0)
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
                val cmdLine = parser.parse(options, commands)
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
                formatter.printHelp("LGFS", options)
            }

        }
    }
}