package lgfs.gfs

import java.nio.ByteBuffer
import java.util.*

class MutationHolder(private val chunkHandle: Long) {
    private val chunkLeases = HashMap<Long, Lease>()

    // maps client id
    private val clientMutations = HashMap<String, MutableList<Mutation>>()
    private lateinit var history: MutableList<ChunkFile.Memento>

    private val originalChunk = ChunkFile(hexHandle(chunkHandle))
    val previousState = originalChunk.save()

    fun addMutation(clientId: String, payloadId: String, offset: Int) {
        if (!clientMutations.containsKey("clientId")) {
            clientMutations[clientId] = ArrayList();
        }
        val mutationSize = clientMutations[clientId]!!.size
        clientMutations[clientId]!!.add(Mutation(payloadId, Mutation.Type.write, offset, mutationSize))
    }

    fun commitMutation(clientId: String, chunkBlocks: HashMap<String, ChunkData>): Boolean {
        if (!chunkLeases.containsKey(chunkHandle)) return TODO()
        val lease = chunkLeases[chunkHandle]!!
        if (!lease.isValid()) return TODO()
        // TODO check that this client has pending mutations
        val mutations = clientMutations[clientId]!!

        mutations.forEach { mutation: Mutation ->
            originalChunk.writeChunk(mutation, chunkBlocks[mutation.mutationId]!!)
        }

        return true
    }

    fun replicate() {

    }

    private fun hexHandle(chunkHandle: Long): String {
        val buffer = ByteBuffer.allocate(8);
        buffer.putLong(chunkHandle)
        val hexFormatter = HexFormat.ofDelimiter("")
        return hexFormatter.formatHex(buffer.array())
    }
}