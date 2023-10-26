package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.FileMetadata
import lgfs.network.FileProtocol
import lgfs.network.Manager
import java.time.Duration
import java.util.concurrent.CompletionStage

class GfsApi(private val gfsMasterService: ActorRef<FileProtocol>, private val system: ActorSystem<Manager.Command>) {
    fun createFile(fileMetadata: FileMetadata): CompletionStage<FileProtocol> {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(gfsMasterService, { replyTo ->
            FileProtocol.CreateFileReq("", fileMetadata, replyTo)
        }, Duration.ofMinutes(100000), system.scheduler())
        return res
    }
}