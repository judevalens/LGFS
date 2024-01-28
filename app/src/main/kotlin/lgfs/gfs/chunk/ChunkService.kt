package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import lgfs.gfs.ChunkData
import lgfs.gfs.FileProtocol
import lgfs.gfs.Lease
import lgfs.gfs.MutationHolder
import lgfs.network.Secrets
import lgfs.network.ServerAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.HexFormat
import kotlin.io.path.Path

class ChunkService(private val chunkAllocatorActor: ActorRef<Allocator.Command>, private val system: ActorSystem<Void>) {
	private val leases = HashMap<Long, Lease>()
	private val mutationHolders = HashMap<Long, MutationHolder>()
	private val mutationData = HashMap<String, ChunkData>()
	private val logger: Logger = LoggerFactory.getLogger(this::class.java)

	companion object {
		private const val ROOT_PATH = "/gfs"
		val ROOT_CHUNK_PATH = Path(ROOT_PATH, Secrets.getSecrets().getHostName())
		fun hexHandle(chunkHandle: Long): String {
			val buffer = ByteBuffer.allocate(8)
			buffer.putLong(chunkHandle)
			val hexFormatter = HexFormat.ofDelimiter("")
			return hexFormatter.formatHex(buffer.array())
		}
	}

	init {
		Files.createDirectories(ROOT_CHUNK_PATH)
	}

	fun addMutations(mutations: List<FileProtocol.Mutation>): Boolean {
		mutations.forEach { mutation ->
			val chunkHandle = mutation.chunkMetadata.handle
			val mutationHolder =
				mutationHolders.getOrPut(chunkHandle) { MutationHolder(chunkHandle, chunkAllocatorActor, system) }
			mutationHolder.addMutation(mutation, isPrimary(mutation.primary))
		}
		return true
	}

	private fun isPrimary(serverAddress: ServerAddress): Boolean {
		return Secrets.getSecrets().getServerAddress() == serverAddress
	}

	fun commitMutations(commitMutationReqs: FileProtocol.CommitMutationReqs): Boolean {
		commitMutationReqs.commitReqs.forEach { commitMutationReq ->
			if (mutationHolders.containsKey(commitMutationReq.chunkHandle)) {
				mutationHolders[commitMutationReq.chunkHandle]!!.commitMutation(
					commitMutationReq.clientId, mutationData, commitMutationReq.replicas
				)
			}
		}
		return true
	}

	fun handlePayloadData(mutationId: String, payload: ByteArray) {
		mutationData[mutationId] = ChunkData(0, "", payload)
	}

	fun handleLeaseGrant(newLeases: List<Lease>) {
		newLeases.forEach {
			val chunkHandle = it.chunkMetadata.handle
			leases[chunkHandle] = it
			if (mutationHolders.containsKey(chunkHandle)) {
				mutationHolders[chunkHandle]?.lease = it
			} else {
				mutationHolders[chunkHandle] = MutationHolder(chunkHandle, chunkAllocatorActor, system)
				mutationHolders[chunkHandle]?.lease = it
			}
		}
	}
}