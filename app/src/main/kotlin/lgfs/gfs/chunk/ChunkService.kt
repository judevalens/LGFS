package lgfs.gfs.chunk

import lgfs.gfs.ChunkData
import lgfs.gfs.FileProtocol
import lgfs.gfs.Lease
import lgfs.gfs.MutationHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkService {
    private val leases = HashMap<Long, Lease>()
    private val mutationHolders = HashMap<Long, MutationHolder>()
    private val mutationData = HashMap<String, ChunkData>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    fun addMutations(mutations: List<FileProtocol.Mutation>): Boolean {
            mutations.forEach { mutation ->
                var mutationHolder = MutationHolder(mutation.chunkHandle)
                if (mutationHolders.containsKey(mutation.chunkHandle)) {
                    mutationHolder = mutationHolders[mutation.chunkHandle]!!
                }
                mutationHolder.addMutation(mutation, isPrimary(mutation.primary))

            }
        return true
    }

    private fun isPrimary(hostname: String): Boolean {
        return TODO()
    }

    fun commitMutation(clientId: String, chunkHandle: Long, replicas: List<String>): Boolean {
        if (mutationHolders.containsKey(chunkHandle)) {
            mutationHolders[chunkHandle]!!.commitMutation(clientId, TODO())
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