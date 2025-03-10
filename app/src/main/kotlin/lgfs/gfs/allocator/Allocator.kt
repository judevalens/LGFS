package lgfs.gfs.allocator

import lgfs.gfs.ChunkMetadata
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileMetadata
import lgfs.gfs.Lease
import lgfs.gfs.chunk.Allocator
import lgfs.network.ServerAddress
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class Allocator {
    private var chunkServerStates = HashMap<ServerAddress, ChunkServerState>()
    private val serverQueue = TreeMap<Double, ArrayList<ServerAddress>>()
    private val chunkIdToRanks: HashMap<ServerAddress, Double> = HashMap()
    private val numReplica = 3
    private val atomicChunkId = AtomicLong()
    private val leases = HashMap<Long, Lease>()
    private val chunkInventory = HashMap<Long, MutableList<ServerAddress>>()

    companion object {
        const val CHUNK_SIZE = 64 * 1000
        val LEASE_DURATION = Duration.ofMinutes(1).toMillis()
    }

    /**
     * Go over the queue and assign n replicas to a chunk
     *
     * //TODO probably need to abstract this method to an interface so we can allocation policy on the fly
     */
    private fun assignChunkToServers(): MutableList<ServerAddress> {
        val replicas = mutableListOf<ServerAddress>()

        for (i in 0 until numReplica) {
            // stops the loop if we don't have chunk server left to replicate the current chunk
            if (serverQueue.isEmpty()) break
            val rank = serverQueue.lastEntry().key
            val replica = serverQueue[rank]!!.random()

            // removing the key completely if it doesn't contain any chunk server hostname
            if (serverQueue[rank]!!.size == 0) {
                serverQueue.remove(rank)
            }
            replicas.add(replica)
        }
        return replicas
    }

    /**
     * Updates the server queue by recomputing the new state's rank
     */
    fun updateState(chunkServerState: ChunkServerState): Boolean {
        val newRank = chunkServerState.getRanking()

        // removing chunk server state from the queue if already present before inserting the new state
        if (chunkIdToRanks.containsKey(chunkServerState.serverAddress)) {
            val currentRank = chunkIdToRanks[chunkServerState.serverAddress]!!
            // removes queued server from tree map to reinsert it with its new ranking
            if (serverQueue.containsKey(currentRank)) {
                val queuedServers = serverQueue[currentRank]!!
                queuedServers.remove(chunkServerState.serverAddress)
            }
        }

        if (serverQueue.containsKey(newRank)) {
            val queuedServers = serverQueue[newRank]!!
            queuedServers.add(chunkServerState.serverAddress)
        } else {
            serverQueue[newRank] = ArrayList(Collections.singleton(chunkServerState.serverAddress))
        }

        chunkIdToRanks[chunkServerState.serverAddress] = newRank
        chunkServerStates[chunkServerState.serverAddress] = chunkServerState
        return true
    }

    fun allocateChunks(fileMetadata: FileMetadata): Pair<Boolean, ArrayList<ChunkMetadata>?> {
        val numChunks = ceil(fileMetadata.size.toDouble() / CHUNK_SIZE).toInt()
        val chunks = ArrayList<ChunkMetadata>()
        for (i in fileMetadata.offset..<fileMetadata.offset + numChunks) {
            chunks.add(
                ChunkMetadata(
                    atomicChunkId.getAndIncrement(),
                    i
                )
            )
        }
        return Pair(
            true,
            chunks
        )
    }

    fun addChunkToInventory(chunkHandle: Long, serverAddress: ServerAddress) {
        if (chunkInventory.containsKey(chunkHandle)) {
            chunkInventory[chunkHandle]?.add(serverAddress)
        } else {
            chunkInventory[chunkHandle] = ArrayList(Collections.singleton(serverAddress))
        }
    }

    /**
     * Grants a lease to chunk servers for a given chunk identified by its handle.
     * Leases are used to mutate chunks (create or update an existing chunk)
     */
    fun grantLease(chunkMetadataList: List<ChunkMetadata>): HashMap<ServerAddress, MutableList<Lease>> {
        val grantedLeases = HashMap<ServerAddress, MutableList<Lease>>()
        chunkMetadataList.forEach { chunkMetadata ->
            var currentLease: Lease? = null
            val chunkHandle = chunkMetadata.handle
            if (leases.containsKey(chunkHandle) && isLeaseValid(leases[chunkHandle]!!)) {
                // grantedLeases.add(leases[it]!!)
                currentLease = leases[chunkHandle]!!
            } else {
                /**
                 * if this chunk is already attributed to a set chunkServers then we'll just grant a new lease to one of the servers
                 * otherwise we assign this chunk to a set of severs
                 */
                if (chunkInventory.containsKey(chunkHandle)) {
                    val replicas = chunkInventory[chunkHandle]!!
                    val primaryAddress = replicas.removeAt(0)
                    currentLease = Lease(
                        primaryAddress,
                        chunkMetadata,
                        Duration.ofMinutes(1).toMillis(),
                        LEASE_DURATION,
                        replicas

                    )
                } else {
                    val replicas = assignChunkToServers()
                    if (replicas.isNotEmpty()) {
                        val primaryAddress = replicas.removeAt(0)
                        currentLease =
                            Lease(
                                primaryAddress,
                                chunkMetadata,
                                Duration.ofMinutes(1).toMillis(),
                                LEASE_DURATION,
                                replicas
                            )
                    }
                }
            }
            if (currentLease != null) {
                if (grantedLeases.containsKey(currentLease.primary)) {
                    grantedLeases[currentLease.primary]!!.add(currentLease)
                } else {
                    grantedLeases[currentLease.primary] = ArrayList(Collections.singleton(currentLease))
                }
            }
        }
        return grantedLeases
    }

    private fun isLeaseValid(lease: Lease): Boolean {
        return true //TODO implement this
    }

    fun updateChunkInventory(chunks: List<Allocator.ChunkEntry>, serverAddress: ServerAddress) {
        chunks.forEach { chunk ->
            if (chunkInventory.containsKey(chunk.handle) && chunkInventory[chunk.handle]!!.contains(serverAddress)) {
                chunkInventory[chunk.handle]?.add(serverAddress)
            }
        }
    }
}