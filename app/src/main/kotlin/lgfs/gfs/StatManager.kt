package lgfs.gfs

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import java.util.*

class StatManager(context: ActorContext<Command>) : AbstractBehavior<StatManager.Command>(context) {
    interface Command {}

    class ReplicaLocationsReq(val fileMetadata: FileMetadata, val replyTo: ActorRef<ReplicaLocationsRes>) : Command
    class ReplicaLocationsRes(val replicationLocations: List<Pair<String, Set<String>>>) : Command

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
            .onMessage(ReplicaLocationsReq::class.java, this::onDequeueServers)
            .build()
    }

    private fun onDequeueServers(msg: ReplicaLocationsReq): Behavior<Command> {
        msg.replyTo.tell(ReplicaLocationsRes(dequeueServers()))
        return Behaviors.same()
    }

    private fun dequeueServers(): List<Pair<String, Set<String>>> {
        return emptyList()
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