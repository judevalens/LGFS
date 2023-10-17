package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.pubsub.Topic
import lgfs.gfs.FileSystem
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

    private val chunkServers = HashMap<String, ActorRef<ClusterProtocol>>()
    private val fs = FileSystem()
    private val protocolTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-protocol"), "cluster-pub-sub")

    init {
        timers.startTimerWithFixedDelay(
            MASTER_UP_TIMER_KEY,
            ClusterProtocol.MasterUP(context.self),
            Duration.ZERO,
            Duration.ofSeconds(15)
        )
    }


    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder()
            .onMessage(ClusterProtocol.MasterUP::class.java, this::onMasterUP)
            .onMessage(ClusterProtocol.ChunkUp::class.java, this::onChunkUp)
            .onMessage(ClusterProtocol.ChunkInventory::class.java, this::onChunkInventory)
            .build()
    }

    private fun onMasterUP(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        protocolTopic.tell(Topic.publish(msg))
        return Behaviors.same()
    }

    private fun onChunkUp(msg: ClusterProtocol.ChunkUp): Behavior<ClusterProtocol> {
        logger.info("Chunk server: {}, is up at path: {}",msg.serverHostName,msg.chunkRef.path())
        chunkServers[msg.serverHostName] = msg.chunkRef
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

}