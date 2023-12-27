package lgfs.gfs.allocator

import lgfs.gfs.ChunkMetadata
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileMetadata
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class Allocator {
    private var chunkServerStates = HashMap<String, ChunkServerState>()
    private val serverQueue = TreeMap<Double, ArrayList<String>>()
    private val chunkIdToRanks: HashMap<String, Double> = HashMap()
    private val numReplica = 3
    private val atomicChunkId = AtomicLong()

    companion object {
        const val CHUNK_SIZE = 64 * 1000 * 1000;
    }

    /**
     * Go over the queue and assign n replicas to a chunk
     * //TODO probably need to abstract this method to an interface so we can allocation policy on the fly
     */
    private fun dequeReplicas(): MutableList<String> {
        val replicas = mutableListOf<String>()

        for (i in 0..numReplica) {
            // stops the loop if we don't have chunk server left to replicate the current chunk
            if (serverQueue.isEmpty()) break
            val rank = serverQueue.firstEntry().key
            val replica = serverQueue[rank]!!.removeAt(0)

            // removing the key completely if it doesn't contain any chunk server hostname
            if (serverQueue[rank]!!.size == 0) {
                serverQueue.remove(rank)
            }
            replicas.add(replica)
        }

        replicas.forEach {
            println(it)
            val chunkServerState = chunkServerStates[it]!!

            // updates the last time this server was selected to host a new chunk so that it can be moved down the queue
            chunkServerState.updateLastAccessed()
            updateState(chunkServerState)
        }
        return replicas
    }

    /**
     * Updates the server queue by recomputing the new state's rank
     */
    fun updateState(chunkServerState: ChunkServerState): Boolean {
        val newRank = chunkServerState.getRanking()

        // removing chunk server state from the queue if already present before inserting the new state
        if (chunkIdToRanks.containsKey(chunkServerState.chunkServerHostName)) {
            val currentRank = chunkIdToRanks[chunkServerState.chunkServerHostName]!!
            // removes queued server from tree map to reinsert it with its new ranking
            if (serverQueue.containsKey(currentRank)) {
                val queuedServers = serverQueue[currentRank]!!
                queuedServers.remove(chunkServerState.chunkServerHostName)
            }
        }

        if (serverQueue.containsKey(newRank)) {
            val queuedServers = serverQueue[newRank]!!
            queuedServers.add(chunkServerState.chunkServerHostName)
        } else {
            serverQueue[newRank] = ArrayList(Collections.singleton(chunkServerState.chunkServerHostName))
        }

        chunkIdToRanks[chunkServerState.chunkServerHostName] = newRank
        chunkServerStates[chunkServerState.chunkServerHostName] = chunkServerState
        return true
    }

    fun allocateChunks(fileMetadata: FileMetadata): Pair<Boolean, ArrayList<ChunkMetadata>?> {
        val numChunks = ceil(fileMetadata.size.toDouble() / CHUNK_SIZE).toInt()
        val chunks = ArrayList<ChunkMetadata>()
        for (i in 0..<numChunks) {
            val replicas = dequeReplicas()
            if (replicas.isEmpty()) {
                return Pair(false, null);
            }
            chunks.add(ChunkMetadata(atomicChunkId.getAndIncrement(), i.toLong(), dequeReplicas()))
        }
        return Pair(true, chunks)
    }
}