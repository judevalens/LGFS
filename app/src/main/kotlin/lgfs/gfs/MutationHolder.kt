package lgfs.gfs

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import lgfs.gfs.chunk.Allocator
import lgfs.network.ServerAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MutationHolder(
	chunkHandle: Long, chunkAllocator: ActorRef<Allocator.Command>, system: ActorSystem<Void>
) {
	private val logger: Logger = LoggerFactory.getLogger(this::class.java)
	private var mutationCounter = 0;
	var lease: Lease? = null

	// maps client id
	private val clientMutations = HashMap<String, MutableList<FileProtocol.Mutation>>()
	private lateinit var history: MutableList<ChunkFile.Memento>

	private var originalChunk: ChunkFile = ChunkFile(chunkHandle, chunkAllocator, system)

	fun addMutation(mutation: FileProtocol.Mutation, setSerial: Boolean) {
		if (!clientMutations.containsKey("clientId")) {
			clientMutations[mutation.clientId] = ArrayList();
		}
		var updatedMutation = mutation
		if (setSerial) {
			updatedMutation = FileProtocol.Mutation(
				mutation.clientId,
				mutation.chunkMetadata,
				mutation.mutationId,
				mutation.primary,
				mutation.replicas,
				getSerial(),
				mutation.offset
			)
		}
		clientMutations[mutation.clientId]!!.add(updatedMutation)
	}

	fun commitMutation(clientId: String, chunkBlocks: HashMap<String, ChunkData>, replicas: List<ServerAddress>): Boolean {
		logger.info("committing mutations from client: {}", clientId)
		// TODO needs to return a more descriptive error
		if ((lease == null) || !isLeaseValid(lease!!)) return false
		// TODO check that this client has pending mutations
		val mutations = clientMutations[clientId]!!
		mutations.forEach { mutation: FileProtocol.Mutation ->
			originalChunk.startTransaction()
			originalChunk.writeChunk(
				mutation, chunkBlocks[mutation.mutationId]!!
			)
			val isCommitted = originalChunk.commitTransaction()
		}
		return true
	}

	fun replicate() {
	}


	private fun getSerial(): Int {
		mutationCounter += 1
		return mutationCounter
	}

	private fun isLeaseValid(lease: Lease): Boolean {
		return true
	}
}