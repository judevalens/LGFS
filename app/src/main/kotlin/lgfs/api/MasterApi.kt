package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.FileMetadata
import lgfs.network.FileProtocol
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletionStage

class MasterApi(private val gfsMasterService: ActorRef<FileProtocol>, private val system: ActorSystem<Void>) {
    fun createFile(fileMetadata: FileMetadata): CompletionStage<FileProtocol> {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(gfsMasterService, { replyTo ->
            FileProtocol.CreateFileReq(UUID.randomUUID().toString(), fileMetadata, replyTo)
        }, Duration.ofMinutes(100000), system.scheduler())
        return res
    }
}