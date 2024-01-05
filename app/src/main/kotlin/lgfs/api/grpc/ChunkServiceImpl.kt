package lgfs.api.grpc

import ChunkServiceGrpcKt
import Gfs
import io.grpc.Server
import io.grpc.ServerBuilder
import lgfs.api.ChunkAPI

class ChunkServiceImpl(val chunkAPI: ChunkAPI) : ChunkServiceGrpcKt.ChunkServiceCoroutineImplBase() {
    val port = 7009;
    val server: Server = ServerBuilder
        .forPort(port)
        .intercept(TagInterceptor(context))
        .addService(this)
        .build()

    fun startServer() {
        server.start()
    }

    override suspend fun addMutations(request: Gfs.Mutations): Gfs.Status {
        return super.addMutations(request)
    }
}