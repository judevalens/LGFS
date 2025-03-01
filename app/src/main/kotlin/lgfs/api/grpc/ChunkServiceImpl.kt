package lgfs.api.grpc

import ChunkServiceGrpcKt
import Gfs
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.Status
import lgfs.api.ChunkAPI
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileProtocol
import lgfs.gfs.chunk.Command
import lgfs.network.Secrets
import lgfs.network.ServerAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkServiceImpl(private val chunkAPI: ChunkAPI) : ChunkServiceGrpcKt.ChunkServiceCoroutineImplBase() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val server: Server = ServerBuilder.forPort(Secrets.getSecrets().getServerAddress().apiPort)
        .intercept(TagInterceptor(context))
        .addService(this)
        .intercept(ExceptionInterceptor(context))
        .build()

    fun startServer() {
        server.start()
    }

    override suspend fun addMutations(request: Gfs.Mutations): Gfs.Status {
        val reqId = TagInterceptor.getRequestId()
        logger.info("req id: {}, Processing add mutations request 2", reqId)
        val gfsMutations = ArrayList<FileProtocol.Mutation>()

        request.mutationsList.forEach {
            val replicaAddresses = ArrayList<ServerAddress>()
            it.replicasList.forEach { addr ->
                replicaAddresses.add(
                    ServerAddress(addr.hostName, addr.akkaPort, addr.apiPort, addr.dataPort),
                )
            }
            gfsMutations.add(
                FileProtocol.Mutation(
                    it.clientId,
                    ChunkMetadata(
                        it.chunk.chunkHandle,
                        it.chunk.chunkIndex.toLong()
                    ),
                    it.mutationId,
                    ServerAddress(it.primary.hostName, it.primary.akkaPort, it.primary.apiPort, it.primary.dataPort),
                    replicaAddresses,
                    it.serial,
                    it.offset
                )
            )
        }
        chunkAPI.addMutations(reqId, gfsMutations)
        logger.info("made api call to add mutations")
        return Gfs.Status.newBuilder()
            .setStatus(Status.OK.code.toString())
            .setCode(Status.OK.code.value())
            .build()
    }

    override suspend fun commitMutations(request: Gfs.CommitMutationReqs): Gfs.Status {
        val reqId = TagInterceptor.getRequestId()
        logger.info("req id: {}, Processing commit mutations request 2", reqId)

        val commits = ArrayList<Command.CommitMutationReq>()

        request.commitsList.forEach { commitReq ->
            val replicas = ArrayList<ServerAddress>()
            commitReq.replicasList.forEach { addr ->
                replicas.add(ServerAddress(addr.hostName, addr.akkaPort, addr.apiPort, addr.dataPort))
            }
            commits.add(Command.CommitMutationReq(reqId, commitReq.clientId, commitReq.chunkHandle, replicas))
        }

        chunkAPI.commitMutations(reqId, commits)

        return Gfs.Status.newBuilder()
            .setStatus(Status.OK.code.toString())
            .setCode(Status.OK.code.value())
            .build()
    }
}