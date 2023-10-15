package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.pubsub.Topic
import akka.cluster.typed.Cluster
import lgfs.gfs.ChunkMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Chunk(context: ActorContext<ClusterProtocol>) : AbstractBehavior<ClusterProtocol>(context) {
    private val protocolTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-protocol"), "cluster-pub-sub")
    private var isInitialized = false
    val cluster: Cluster = Cluster.get(context.system)
    private lateinit var masterRef: ActorRef<ClusterProtocol>
    private val chunks = HashMap<Long, ChunkMetadata>()

    init {
        protocolTopic.tell(Topic.subscribe(context.self))
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        fun create(): Behavior<ClusterProtocol> {
            return Behaviors.setup {
                logger.info("created lgs chunk actor")
                Chunk(it)
            }
        }
    }

    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder()
            .onMessage(ClusterProtocol.MasterUP::class.java, this::handleMasterUp)
            .onMessage(ClusterProtocol.RequestChunkInventory::class.java, this::onRequestChunkInventory)
            .build()
    }

    private fun handleMasterUp(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        masterRef = msg.masterRef
        masterRef.tell(ClusterProtocol.ChunkUp(context.self, Secrets.getSecrets().getHostName()))
        isInitialized = true
        return Behaviors.same()
    }

    private fun onRequestChunkInventory(msg: ClusterProtocol.RequestChunkInventory): Behavior<ClusterProtocol> {
        if (!isInitialized) return Behaviors.same()
        val chunksIterator = chunks.values.iterator()
        while (chunksIterator.hasNext()) {
            val inventory = ClusterProtocol.ChunkInventory(Secrets.getSecrets().getHostName(), ArrayList())
            var payloadSize = 0
            while (chunksIterator.hasNext() && payloadSize < 4096) {
                payloadSize += 8;
                inventory.chunkIds.add(chunksIterator.next().handle)
            }
            masterRef.tell(inventory)
        }
        return Behaviors.same()
    }
}