package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.FileProtocol
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletionStage

class ChunkAPI(private val chunkService: ActorRef<FileProtocol>, private val system: ActorSystem<Void>) {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	fun addMutations(reqId: String, mutations: List<FileProtocol.Mutation>) {
			logger.info("Send add mutation msg to chunkService actor")
			val	 res: CompletionStage<FileProtocol> = AskPattern.ask(
			chunkService, {
				FileProtocol.Mutations(reqId, mutations)
			},
			Duration.ofSeconds(100000),
			system.scheduler()
		)
	}
}