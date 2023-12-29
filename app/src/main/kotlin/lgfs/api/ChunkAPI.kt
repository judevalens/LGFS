package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.FileProtocol
import java.time.Duration
import java.util.concurrent.CompletionStage

class ChunkAPI(private val chunkService: ActorRef<FileProtocol>, private val system: ActorSystem<Void>) {
    fun addMutations(mutations: List<FileProtocol.Mutation>) {
        val res: CompletionStage<FileProtocol> = AskPattern.ask(
            chunkService, {
                FileProtocol.Mutations(mutations)
            },
            Duration.ofDays(100000),
            system.scheduler()
        )
    }
}