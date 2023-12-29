package lgfs.gfs.allocator

import lgfs.gfs.ChunkMetadata
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileMetadata
import lgfs.gfs.Lease
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class Allocator {
    private var chunkServerStates = HashMap<String, ChunkServerState>()
    private val serverQueue = TreeMap<Double, ArrayList<String>>()
    private val chunkIdToRanks: HashMap<String, Double> = HashMap()
    private val numReplica = 3
    private val atomicChunkId = AtomicLong()

    private val leases = HashMap<Long, Lease>()
    private val chunkInventory = HashMap<Long, MutableList<String>>()

    class LeaseHolder {
        val leases = HashMap<Long, MutableSet<String>>()
    }

    companion object {
        const val CHUNK_SIZE = 64 * 1000 * 1000;
    }

    /**
     * Go over the queue and assign n replicas to a chunk
     * //TODO probably need to abstract this method to an interface so we can allocation policy on the fly
     */
    private fun assignChunkToServers(): MutableList<String> {
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
            /*
            this is probably not needed
            val replicas = assignChunkToServers()
            if (replicas.isEmpty()) {
                return Pair(false, null);
            }*/
            chunks.add(ChunkMetadata(atomicChunkId.getAndIncrement(), i))
        }
        return Pair(true, chunks)
    }

    fun addChunkToInventory(chunkHandle: Long, serverHostname: String) {
        if (chunkInventory.containsKey(chunkHandle)) {
            chunkInventory[chunkHandle]?.add(serverHostname)
        } else {
            chunkInventory[chunkHandle] = ArrayList(Collections.singleton(serverHostname))
        }
    }

    /**
     * Grants a lease to chunk servers for a given chunk identified by its handle.
     * Leases are used to mutate chunks (create or update an existing chunk)
     */
    fun grantLease(chunkHandles: List<Long>): ArrayList<Lease> {
        val grantedLeases = ArrayList<Lease>()
        chunkHandles.forEach {
            if (leases.containsKey(it) && isLeaseValid(leases[it]!!)) {
                grantedLeases.add(leases[it]!!)
            } else {
                /**
                 * if this chunk is already attributed to a set chunkServers then we'll just grant a new lease to one of the servers
                 * otherwise we assign this chunk to a set of severs
                 */
                if (chunkInventory.containsKey(it)) {
                    val replicas = chunkInventory[it]!!
                    val primary = replicas.removeAt(0)
                    grantedLeases.add(Lease(it, Duration.ofMinutes(1).toMinutes(), primary, replicas))
                }else {
                    val replicas = assignChunkToServers()
                    if (replicas.isNotEmpty()) {
                        val primary = replicas.removeAt(0)
                        grantedLeases.add(Lease(it, Duration.ofMinutes(1).toMinutes(), primary, replicas))
                    }
                }
            }
        }
        return grantedLeases
    }

    private fun isLeaseValid(lease: Lease): Boolean {
        return true //TODO implement this
    }
}