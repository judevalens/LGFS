package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.pubsub.Topic
import akka.cluster.typed.Cluster
import lgfs.gfs.ChunkMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class ChunkServer(context: ActorContext<ClusterProtocol>, timers: TimerScheduler<ClusterProtocol>) :
    AbstractBehavior<ClusterProtocol>(context) {
    private class SendChunkUp : ClusterProtocol

    private val protocolTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-protocol"), "cluster-pub-sub")
    private var isInitialized = false
    val cluster: Cluster = Cluster.get(context.system)
    private lateinit var masterRef: ActorRef<ClusterProtocol>
    private val chunks = HashMap<Long, ChunkMetadata>()
    private val chunkServers = HashMap<String, ActorRef<ClusterProtocol>>()

    init {
        protocolTopic.tell(Topic.subscribe(context.self))

        // Periodically send itself to other chunk servers via a pub-sub actor
        timers.startTimerWithFixedDelay(
            CHUNK_UP_TIMER_KEY,
            SendChunkUp(),
            Duration.ZERO,
            Duration.ofSeconds(15)
        )

    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val CHUNK_UP_TIMER_KEY = "chunk_up_timer"

        fun create(): Behavior<ClusterProtocol> {
            return Behaviors.setup {
                Behaviors.withTimers { timer ->
                    logger.info("created lgs chunk actor")
                    ChunkServer(it, timer)
                }
            }
        }
    }

    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder()
            .onMessage(SendChunkUp::class.java, this::onSendChunkUp)
            .onMessage(ClusterProtocol.MasterUP::class.java, this::handleMasterUp)
            .onMessage(ClusterProtocol.RequestChunkInventory::class.java, this::onRequestChunkInventory)
            .onMessage(ClusterProtocol.ChunkUp::class.java, this::handleChunkUp)
            .build()
    }

    private fun handleMasterUp(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        logger.info("Master is up at path {}", msg.masterRef.path())
        masterRef = msg.masterRef
        masterRef.tell(ClusterProtocol.ChunkUp(context.self, Secrets.getSecrets().getHostName()))
        isInitialized = true
        return Behaviors.same()
    }

    private fun handleChunkUp(msg: ClusterProtocol.ChunkUp): Behavior<ClusterProtocol> {
        logger.info("Chunk server is up at address : ${msg.chunkRef.path().address()}")
        chunkServers[msg.serverHostName] = msg.chunkRef
        return Behaviors.same()
    }

    /**
     *
     */
    private fun onSendChunkUp(msg: SendChunkUp): Behavior<ClusterProtocol> {
        protocolTopic.tell(Topic.publish(ClusterProtocol.ChunkUp(context.self, Secrets.getSecrets().getHostName())))
        return Behaviors.same()
    }

    private fun onRequestChunkInventory(msg: ClusterProtocol.RequestChunkInventory): Behavior<ClusterProtocol> {
        if (!isInitialized) return Behaviors.same()
        logger.info("Sending chunk inventory to master")
        val chunksIterator = chunks.values.iterator()
        while (chunksIterator.hasNext()) {
            val inventory = ClusterProtocol.ChunkInventory(Secrets.getSecrets().getHostName(), ArrayList())
            var payloadSize = 0
            while (chunksIterator.hasNext() && payloadSize < 4096) {
                payloadSize += 8
                inventory.chunkIds.add(chunksIterator.next().handle)
            }
            masterRef.tell(inventory)
        }
        return Behaviors.same()
    }
}