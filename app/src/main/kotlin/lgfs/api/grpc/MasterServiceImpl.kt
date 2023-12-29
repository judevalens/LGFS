package lgfs.api.grpc

import Gfs
import MasterServiceGrpcKt
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.future.await
import lgfs.api.MasterApi
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileProtocol


class MasterServiceImpl(private val masterGfs: MasterApi) : MasterServiceGrpcKt.MasterServiceCoroutineImplBase() {
    override suspend fun createFile(request: Gfs.CreateFileReq): Gfs.CreateFileRes {
        val res = masterGfs.createFile(FileMetadata(request.fileName, false, request.fileSize.toLong()))
            .await() as? FileProtocol.CreateFileRes
        res?.let { createFileRes ->
            val grpcChunks = mutableListOf<Gfs.Chunk>()
            createFileRes.chunks?.forEach {
                grpcChunks.add(
                    Gfs.Chunk.newBuilder().setChunkHandle(it.handle).setChunkIndex(it.index).build()
                )
            }
            return Gfs.CreateFileRes.newBuilder().setIsSuccessful(createFileRes.successful).addAllChunks(grpcChunks)
                .build()
        }

        return Gfs.CreateFileRes.newBuilder().setIsSuccessful(false).build()
    }

    override suspend fun getLease(request: Gfs.LeaseGrantReq): Gfs.LeaseGrantRes {
        val res = masterGfs.getLease(request.chunkHandlesList).await() as? FileProtocol.LeaseGrantRes
        val leases = ArrayList<Gfs.Lease>()

        res?.let {
            it.leases.forEach { lease ->
                val grpcLease = Gfs.Lease.newBuilder()
                    .setChunkHandle(lease.chunkId)
                    .setPrimary(lease.primary)
                    .addAllReplicas(lease.replicas)
                    .setGrantedAt(lease.ts)
                    .setDuration(lease.duration)
                    .build()
                leases.add(grpcLease)
            }

            return Gfs.LeaseGrantRes.newBuilder().addAllLeases(leases).build()
        }
        //TODO handle incorrect datatype
        return super.getLease(request)
    }

    val port = 7009
    val server: Server = ServerBuilder.forPort(port).addService(this).build()

    fun startServer() {
        server.start()
    }
}