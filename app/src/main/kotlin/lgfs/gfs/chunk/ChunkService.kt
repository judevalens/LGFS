package lgfs.gfs.chunk

import lgfs.gfs.ChunkData
import lgfs.gfs.FileProtocol
import lgfs.gfs.Lease
import lgfs.gfs.MutationHolder
import lgfs.network.Secrets
import lgfs.network.ServerAddress
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkService {
	private val leases = HashMap<Long, Lease>()
	private val mutationHolders = HashMap<Long, MutationHolder>()
	private val mutationData = HashMap<String, ChunkData>()
	private val logger: Logger = LoggerFactory.getLogger(this::class.java)

	fun addMutations(mutations: List<FileProtocol.Mutation>): Boolean {
		mutations.forEach { mutation ->
			val chunkHandle = mutation.chunkMetadata.handle
			val mutationHolder = mutationHolders.getOrPut(chunkHandle) { MutationHolder(chunkHandle) }
			mutationHolder.addMutation(mutation, isPrimary(mutation.primary))
		}
		return true
	}

	private fun isPrimary(serverAddress: ServerAddress): Boolean {
		return Secrets.getSecrets().getServerAddress() == serverAddress
	}

	fun commitMutation(clientId: String, chunkHandle: Long, replicas: List<ServerAddress>): Boolean {
		if (mutationHolders.containsKey(chunkHandle)) {
			mutationHolders[chunkHandle]!!.commitMutation(clientId, mutationData, replicas)
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
				mutationHolders[chunkHandle] = MutationHolder(chunkHandle)
				mutationHolders[chunkHandle]?.lease = it
			}
		}
	}
}