package lgfs.gfs

import lgfs.network.Secrets
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.*

class MutationHolder(private val chunkHandle: Long) {
    private val chunkLeases = HashMap<Long, Lease>()

    // maps client id
    val clientMutations = HashMap<String, MutableList<Mutation>>()

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
            writeChunk(mutation, chunkBlocks[mutation.mutationId]!!)
        }

        return true
    }

    private fun writeChunk(mutation: Mutation, chunkData: ChunkData) {
        val buffer = ByteBuffer.allocate(8);
        buffer.putLong(chunkHandle)
        val hexFormatter = HexFormat.ofDelimiter("")
        val chunkHexHandle = hexFormatter.formatHex(buffer.array())
        val chunkPath = Paths.get(Secrets.getSecrets().getHomeDir(), chunkHexHandle)
        val chunkFile = chunkPath.toFile()
        chunkFile.outputStream().write(chunkData.data, mutation.offset, chunkData.data.size)
    }
}