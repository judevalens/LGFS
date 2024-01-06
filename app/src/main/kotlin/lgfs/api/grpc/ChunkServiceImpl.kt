package lgfs.api.grpc

import ChunkServiceGrpcKt
import Gfs
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import lgfs.api.ChunkAPI
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileProtocol
import lgfs.network.Secrets
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkServiceImpl(private val chunkAPI: ChunkAPI) : ChunkServiceGrpcKt.ChunkServiceCoroutineImplBase() {
	private val logger: Logger = LoggerFactory.getLogger(this::class.java)
	val server: Server = ServerBuilder.forPort(Secrets.getSecrets().getServerAddress().apiPort)
		.intercept(TagInterceptor(context))
		.addService(this)
		.build()

	fun startServer() {
		server.start()
	}

	override suspend fun addMutations(request: Gfs.Mutations): Gfs.Status {
		val reqId = TagInterceptor.getRequestId()
		logger.info("req id: {}, Processing add mutations request", reqId)
		val gfsMutations = ArrayList<FileProtocol.Mutation>()
		request.mutationsList.forEach {

			gfsMutations.add(
				FileProtocol.Mutation(
					it.clientId,
					ChunkMetadata(
						it.chunk.chunkHandle,
						it.chunk.chunkIndex
					),
					it.mutationId,
					it.primary,
					it.replicasList,
					it.serial,
					it.offset
				)
			)
		}
		chunkAPI.addMutations(gfsMutations)
		return Gfs.Status.newBuilder()
			.setStatus(Status.OK.description)
			.setCode(Status.OK.code.value())
			.build()
	}

}