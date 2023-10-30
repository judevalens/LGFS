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

class StatManager(context: ActorContext<Command>) : AbstractBehavior<StatManager.Command>(context) {
    interface Command {}

    val CHUNK_SIZE = 64 * 1000 * 1000;
    val atomicChunkId = AtomicLong()

    class ChunkAllocationReq(val fileMetadata: FileMetadata, val replyTo: ActorRef<ChunkAllocationRes>) : Command
    class ChunkAllocationRes(val replicationLocations: List<ChunkMetadata>) : Command

    private var chunkServers = HashMap<String, ChunkServerStat>()
    private val serverQueue = TreeMap<Double, ArrayList<String>>()
    private val chunkIdToRanks: HashMap<String, Double> = HashMap()
    private val numReplica = 3

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup {
                StatManager(it)
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(ChunkAllocationReq::class.java, this::onAllocateChunks)
            .build()
    }


    private fun getReplicas(): MutableList<String> {
        return ArrayList()
    }

    private fun onAllocateChunks(msg: ChunkAllocationReq): Behavior<Command> {
        val fileMetadata = msg.fileMetadata
        val numChunks = ceil(fileMetadata.size.toDouble() / CHUNK_SIZE).toInt()
        val replicas = ArrayList<ChunkMetadata>()
        for (i in 0..numChunks) {
            replicas.add(ChunkMetadata(atomicChunkId.getAndDecrement(), getReplicas()))
        }
        msg.replyTo.tell(ChunkAllocationRes(replicas))
        return Behaviors.same()
    }


    fun updateStat(chunkServerStat: ChunkServerStat): Boolean {
        val rank = chunkServerStat.getRanking()

        if (chunkIdToRanks.containsKey(chunkServerStat.chunkServerHostName)) {
            val currentRank = chunkIdToRanks[chunkServerStat.chunkServerHostName]!!
            // removes queued server from tree map to reinsert it with its new ranking
            val queuedServers = serverQueue[currentRank] ?: ArrayList()
            queuedServers.remove(chunkServerStat.chunkServerHostName)
        }

        if (serverQueue.containsKey(rank)) {
            val updatedServers = serverQueue[rank]!!
            updatedServers.add(chunkServerStat.chunkServerHostName)
        } else {
            serverQueue[rank] = ArrayList(Collections.singleton(chunkServerStat.chunkServerHostName))
        }
        chunkIdToRanks[chunkServerStat.chunkServerHostName] = rank
        return true
    }
}