package lgfs.gfs

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class StateManager(context: ActorContext<Command>) : AbstractBehavior<StateManager.Command>(context) {
    interface Command {}

    private val CHUNK_SIZE = 64 * 1000 * 1000;
    private val atomicChunkId = AtomicLong()

    class ChunkAllocationReq(val fileMetadata: FileMetadata, val replyTo: ActorRef<ChunkAllocationRes>) : Command
    class ChunkAllocationRes(val isSuccessful: Boolean, val replicationLocations: List<ChunkMetadata>) : Command
    class UpdateServerState(val state: ChunkServerState) : Command

    private var chunkServers = HashMap<String, ChunkServerState>()
    private val serverQueue = TreeMap<Double, ArrayList<String>>()
    private val chunkIdToRanks: HashMap<String, Double> = HashMap()
    private val numReplica = 3

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup {
                StateManager(it)
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(ChunkAllocationReq::class.java, this::onAllocateChunks)
            .build()
    }

    private fun dequeReplicas(): MutableList<String> {
        val replicas = mutableListOf<String>()
        for (i in 0..numReplica) {
            if (serverQueue.isEmpty()) break
            val key = serverQueue.firstEntry().key
            val replica = serverQueue[key]!!.removeAt(0)
            if (serverQueue[key]!!.size == 0) {
                serverQueue.remove(key)
            }
            replicas.add(replica)
        }

        replicas.forEach {
            val chunkServerState = chunkServers[it]!!
            chunkServerState.updateLastAccessed()
            updateState(chunkServers[it]!!)
        }
        return replicas
    }

    private fun onAllocateChunks(msg: ChunkAllocationReq): Behavior<Command> {
        val fileMetadata = msg.fileMetadata
        val numChunks = ceil(fileMetadata.size.toDouble() / CHUNK_SIZE).toInt()
        val chunks = ArrayList<ChunkMetadata>()
        for (i in 0..numChunks) {
            val replicas = dequeReplicas()
            if (replicas.isEmpty()) {
                msg.replyTo.tell(ChunkAllocationRes(false, emptyList()))
            }
            chunks.add(ChunkMetadata(atomicChunkId.getAndDecrement(), dequeReplicas()))
        }
        msg.replyTo.tell(ChunkAllocationRes(true, chunks))
        return Behaviors.same()
    }

    private fun onUpdateServerState(msg: UpdateServerState) {
        chunkServers[msg.state.chunkServerHostName] = msg.state
        val newRanking = msg.state.getRanking()
    }

    private fun updateState(chunkServerState: ChunkServerState): Boolean {
        val rank = chunkServerState.getRanking()

        if (chunkIdToRanks.containsKey(chunkServerState.chunkServerHostName)) {
            val currentRank = chunkIdToRanks[chunkServerState.chunkServerHostName]!!
            // removes queued server from tree map to reinsert it with its new ranking
            val queuedServers = serverQueue[currentRank] ?: ArrayList()
            queuedServers.remove(chunkServerState.chunkServerHostName)
        }

        if (serverQueue.containsKey(rank)) {
            val updatedServers = serverQueue[rank]!!
            updatedServers.add(chunkServerState.chunkServerHostName)
        } else {
            serverQueue[rank] = ArrayList(Collections.singleton(chunkServerState.chunkServerHostName))
        }
        chunkIdToRanks[chunkServerState.chunkServerHostName] = rank
        return true
    }
}