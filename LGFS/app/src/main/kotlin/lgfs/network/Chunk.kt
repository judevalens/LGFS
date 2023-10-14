package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.typed.Cluster
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Chunk(context: ActorContext<ClusterProtocol>) : AbstractBehavior<ClusterProtocol>(context) {
    private val protocolTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-protocol"), "cluster-pub-sub")
    private val masterServiceKey: ServiceKey<ClusterProtocol.Handshake> = ServiceKey.create(
        ClusterProtocol.Handshake::class.java, "master"
    )
    private var isInitialized = true
    val cluster: Cluster = Cluster.get(context.system)
    private lateinit var masterRef: ActorRef<ClusterProtocol>

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
            .onMessage(ClusterProtocol.MasterUP::class.java) {
                Behaviors.same<ClusterProtocol>()
            }
            .build()
    }

    private fun handleMasterUp(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        if (!isInitialized) {
            masterRef = msg.masterRef
            masterRef.tell(ClusterProtocol.ChunkUp(context.self))
            isInitialized = true
        }
        return Behaviors.same()
    }
}