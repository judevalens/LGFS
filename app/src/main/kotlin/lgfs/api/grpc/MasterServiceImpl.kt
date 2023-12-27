package lgfs.api.grpc

import Gfs
import MasterServiceGrpcKt
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.future.await
import lgfs.api.MasterApi
import lgfs.gfs.FileMetadata
import lgfs.network.FileProtocol


class MasterServiceImpl(private val masterGfs: MasterApi) : MasterServiceGrpcKt.MasterServiceCoroutineImplBase() {
    override suspend fun createFile(request: Gfs.CreateFileReq): Gfs.CreateFileRes {
        val res = masterGfs.createFile(FileMetadata(request.fileName, false, request.fileSize.toLong()))
            .await() as? FileProtocol.CreateFileRes
        res?.let { createFileRes ->
            val grpcChunks = mutableListOf<Gfs.Chunk>()
            createFileRes.chunks?.forEach {
                grpcChunks.add(
                    Gfs.Chunk.newBuilder()
                        .setChunkHandle(it.handle)
                        .setChunkIndex(it.index)
                        .addAllReplicas(it.replicas)
                        .build()
                )
            }
            return Gfs.CreateFileRes.newBuilder()
                .setIsSuccessful(createFileRes.successful)
                .addAllChunks(grpcChunks)
                .build()
        }

        return Gfs.CreateFileRes.newBuilder().setIsSuccessful(false).build()
    }

    val port = 7009
    val server: Server = ServerBuilder.forPort(port).addService(this).build()

    fun startServer() {
        server.start()
    }
}