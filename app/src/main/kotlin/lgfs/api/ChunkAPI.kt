package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import lgfs.gfs.FileProtocol
import lgfs.gfs.chunk.Command
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletionStage

class ChunkAPI(private val chunkService: ActorRef<FileProtocol>, private val system: ActorSystem<Void>) {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	fun addMutations(reqId: String, mutations: List<FileProtocol.Mutation>) {
		logger.info("reqId: {}, Sending add mutation msg to chunkService actor", reqId)
		val res: CompletionStage<FileProtocol> = AskPattern.ask(
			chunkService, {
				FileProtocol.AddMutationsReq(reqId, mutations,it)
			},
			Duration.ofSeconds(100000),
			system.scheduler()
		)
	}

	suspend fun commitMutations(reqId: String, commits: List<Command.CommitMutationReq>) {
		logger.info("reqId: {}, Sending commit mutation msg to chunkService actor", reqId)
		val res: CompletionStage<Command> = AskPattern.ask(
			chunkService, {
				FileProtocol.CommitMutation(reqId, commits)
			},
			Duration.ofSeconds(100000),
			system.scheduler()
		)
	}
}