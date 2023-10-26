package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.pubsub.Topic
import lgfs.gfs.ChunkServerStat
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileSystem
import lgfs.gfs.StatManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration


class Master(context: ActorContext<ClusterProtocol>, timers: TimerScheduler<ClusterProtocol>) :
    AbstractBehavior<ClusterProtocol>(context) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val MASTER_UP_TIMER_KEY = "master-up-timer"
        fun create(): Behavior<ClusterProtocol> {
            return Behaviors.setup { context ->
                logger.info("created lgs master actor")
                Behaviors.withTimers { timers ->
                    Master(context, timers)
                }
            }
        }
    }

    private var fsRequestId = 0L
    private val chunkServers = HashMap<String, ChunkServerStat>()
    private val fs = FileSystem()
    private val statManager: ActorRef<StatManager.Command>
    private val protocolTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-protocol"), "cluster-pub-sub")

    private val reqIds = HashMap<Long, Long>()

    init {
        statManager = context.spawn(StatManager.create(), "ChunkServerStatAggregator")
        // sends heartbeat to cluster ?
        timers.startTimerWithFixedDelay(
            MASTER_UP_TIMER_KEY,
            ClusterProtocol.MasterUP(context.self),
            Duration.ZERO,
            Duration.ofSeconds(2)
        )
    }

    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder()
            .onMessage(ClusterProtocol.MasterUP::class.java, this::onMasterUP)
            .onMessage(ClusterProtocol.ChunkUp::class.java, this::onChunkUp)
            .onMessage(ClusterProtocol.ChunkInventory::class.java, this::onChunkInventory)
            .build()
    }

    private fun createFile(fileMetadata: FileMetadata) {
    }

    private fun onMasterUP(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        protocolTopic.tell(Topic.publish(msg))
        return Behaviors.same()
    }

    private fun onChunkUp(msg: ClusterProtocol.ChunkUp): Behavior<ClusterProtocol> {
        logger.info("Chunk server: {}, is up at path: {}", msg.serverHostName, msg.chunkRef.path())
        chunkServers[msg.serverHostName] = ChunkServerStat(msg.serverHostName)
        msg.chunkRef.tell(ClusterProtocol.RequestChunkInventory())
        return Behaviors.same()
    }

    private fun onChunkInventory(msg: ClusterProtocol.ChunkInventory): Behavior<ClusterProtocol> {
        logger.info("received chunk inventory from {}: {} chunk received", msg.serverHostName, msg.chunkIds.size)
        msg.chunkIds.forEach { chunkId ->
            fs.attachServerToChunk(msg.serverHostName, chunkId)
        }
        return Behaviors.same()
    }

    private fun getReqId(): Long {
        return 0L
    }
}