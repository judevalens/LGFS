package lgfs.gfs

import java.nio.ByteBuffer
import java.util.*

class MutationHolder(private val chunkHandle: Long) {
    private val chunkLeases = HashMap<Long, Lease>()
    private var mutationCounter = 0;

    // maps client id
    private val clientMutations = HashMap<String, MutableList<FileProtocol.Mutation>>()
    private lateinit var history: MutableList<ChunkFile.Memento>

    private val originalChunk = ChunkFile(hexHandle(chunkHandle))
    val previousState = originalChunk.save()

    fun addMutation(mutation: FileProtocol.Mutation, setSerial: Boolean) {
        if (!clientMutations.containsKey("clientId")) {
            clientMutations[mutation.clientId] = ArrayList();
        }
        var updatedMutation = mutation
        if (setSerial) {
            updatedMutation =  FileProtocol.Mutation(
                mutation.clientId,
                mutation.chunkHandle,
                mutation.mutationId,
                mutation.primary,
                mutation.replicas,
                getSerial(),
                mutation.offset
            )
        }
        clientMutations[mutation.clientId]!!.add(updatedMutation)
    }

    fun commitMutation(clientId: String, chunkBlocks: HashMap<String, ChunkData>): Boolean {
        if (!chunkLeases.containsKey(chunkHandle)) return TODO()
        val lease = chunkLeases[chunkHandle]!!
        if (!lease.isValid()) return TODO()
        // TODO check that this client has pending mutations
        val mutations = clientMutations[clientId]!!
        mutations.forEach { mutation: FileProtocol.Mutation ->
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

    private fun getSerial(): Int {
        mutationCounter += 1
        return mutationCounter
    }
}