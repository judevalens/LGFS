package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileProtocol
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletionStage

class MasterApi(private val gfsMasterService: ActorRef<FileProtocol>, private val system: ActorSystem<Void>) {
    fun createFile(reqId: String, fileMetadata: FileMetadata): CompletionStage<FileProtocol> {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(gfsMasterService, { replyTo ->
            FileProtocol.CreateFileReq(reqId, fileMetadata, replyTo)
        }, Duration.ofMinutes(100000), system.scheduler())
        return res
    }

    fun deleteFile(reqId: String, fileName: String): CompletionStage<FileProtocol> {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(gfsMasterService, { replyTo ->
            FileProtocol.DeleteFileReq(UUID.randomUUID().toString(), fileName, replyTo)
        }, Duration.ofMinutes(100000), system.scheduler())
        return res
    }

    fun getLease(reqId: String, chunkHandles: List<ChunkMetadata>): CompletionStage<FileProtocol> {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(gfsMasterService, { replyTo ->
            FileProtocol.LeaseGrantReq(UUID.randomUUID().toString(), chunkHandles, replyTo)
        }, Duration.ofMinutes(100000), system.scheduler())
        return res
    }
}