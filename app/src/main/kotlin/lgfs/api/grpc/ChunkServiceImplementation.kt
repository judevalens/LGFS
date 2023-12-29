package lgfs.api.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import lgfs.api.ChunkAPI

class ChunkServiceImplementation(val chunkAPI: ChunkAPI) : ChunkServiceGrpcKt.ChunkServiceCoroutineImplBase() {
    val port = 7009;
    val server: Server = ServerBuilder
        .forPort(port)
        .addService(this)
        .build()
    fun startServer() {
        server.start()
    }

    override suspend fun addMutations(request: Gfs.Mutations): Gfs.Status {
        return super.addMutations(request)
    }
}