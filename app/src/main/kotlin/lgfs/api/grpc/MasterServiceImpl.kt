package lgfs.api.grpc

import Gfs
import MasterServiceGrpcKt
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.await
import lgfs.api.MasterApi
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileProtocol
import lgfs.network.Secrets
import org.slf4j.LoggerFactory

class MasterServiceImpl(private val masterGfs: MasterApi) : MasterServiceGrpcKt.MasterServiceCoroutineImplBase() {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	override suspend fun createFile(request: Gfs.CreateFileReq): Gfs.CreateFileRes {
		val reqIdElement = currentCoroutineContext()[TagInterceptor.ReqIdKey]
		val reqId = reqIdElement!!.keyStr
		logger.info(
			"reqId: {}, received create file request",
			reqId
		)
		val res = masterGfs.createFile(
			reqId,
			FileMetadata(
				request.fileName,
				false,
				request.fileSize.toLong()
			)
		)
			.await() as? FileProtocol.CreateFileRes
		res?.let { createFileRes ->
			val grpcChunks = mutableListOf<Gfs.Chunk>()
			createFileRes.chunks?.forEach {
				grpcChunks.add(
					Gfs.Chunk.newBuilder()
						.setChunkHandle(it.handle)
						.setChunkIndex(it.index)
						.build()
				)
			}
			return Gfs.CreateFileRes.newBuilder()
				.setIsSuccessful(createFileRes.successful)
				.addAllChunks(grpcChunks)
				.build()
		}

		return Gfs.CreateFileRes.newBuilder()
			.setIsSuccessful(false)
			.build()
	}

	override suspend fun deleteFile(request: Gfs.DeleteFileReq): Gfs.Status {
		val reqIdElement = currentCoroutineContext()[TagInterceptor.ReqIdKey]
		val reqId = reqIdElement!!.keyStr
		logger.info(
			"reqId: {}, received delete file request: {}",
			reqId,
			request.fileName
		)
		val res = masterGfs.deleteFile(
			reqId,
			request.fileName
		)
			.await() as? FileProtocol.DeleteFileRes
		return Gfs.Status.newBuilder()
			.setCode(Status.OK.code.value())
			.setStatus(Status.OK.code.toString())
			.build()
	}

	override suspend fun getLease(request: Gfs.LeaseGrantReq): Gfs.LeaseGrantRes {
		val reqIdElement = currentCoroutineContext()[TagInterceptor.ReqIdKey]
		val reqId = reqIdElement!!.keyStr

		logger.info(
			"reqId: {}, received lease grant request",
			reqId
		)

		val chunkList = ArrayList<ChunkMetadata>(request.chunksList.size)
		request.chunksList.forEach {
			chunkList.add(
				ChunkMetadata(
					it.chunkHandle,
					it.chunkIndex
				)
			)
		}
		val res = masterGfs.getLease(
			reqId,
			chunkList
		)
			.await() as? FileProtocol.LeaseGrantRes
		val leases = ArrayList<Gfs.Lease>()

		res?.let {
			it.leases.forEach { lease ->
				val gfsReplicas = ArrayList<Gfs.ServerAddress>()

				lease.replicas.forEach { serverAddress ->
					gfsReplicas.add(
						Gfs.ServerAddress.newBuilder()
							.setHostName(serverAddress.hostName)
							.setAkkaPort(serverAddress.akkaPort)
							.setDataPort(serverAddress.dataPort)
							.setApiPort(serverAddress.apiPort)
							.build()
					)
				}

				val gfsChunk = Gfs.Chunk.newBuilder()
					.setChunkHandle(lease.chunkMetadata.handle)
					.setChunkIndex(lease.chunkMetadata.index)
					.build()
				val grpcLease = Gfs.Lease.newBuilder()
					.setChunk(gfsChunk)
					.setPrimary(
						Gfs.ServerAddress.newBuilder()
							.setHostName(lease.primary.hostName)
							.setAkkaPort(lease.primary.akkaPort)
							.setApiPort(lease.primary.apiPort)
							.setDataPort(lease.primary.dataPort)
							.build()
					)
					.addAllReplicas(gfsReplicas)
					.setGrantedAt(lease.ts)
					.setDuration(lease.duration)
					.build()
				leases.add(grpcLease)
			}
			return Gfs.LeaseGrantRes.newBuilder()
				.addAllLeases(leases)
				.build()
		}
		//TODO handle incorrect datatype
		return super.getLease(request)
	}

	val server: Server = ServerBuilder.forPort(Secrets.getSecrets().getServerAddress().apiPort)
		.intercept(TagInterceptor(context))
		.addService(this)
		.build()

	fun startServer() {
		server.start()
	}
}