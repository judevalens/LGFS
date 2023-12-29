package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.ClusterEvent
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import lgfs.api.MasterApi
import lgfs.api.grpc.MasterServiceImpl
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileSystem
import lgfs.gfs.allocator.AllocatorActor
import lgfs.gfs.allocator.AllocatorProtocol
import lgfs.gfs.master.MasterServiceActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*


class Master(context: ActorContext<ClusterProtocol>, timers: TimerScheduler<ClusterProtocol>) :
    AbstractBehavior<ClusterProtocol>(context) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val MASTER_UP_TIMER_KEY = "master-up-timer"
        private val serviceKey = ServiceKey.create(ClusterProtocol::class.java, Secrets.getSecrets().getName())
        fun create(): Behavior<ClusterProtocol> {
            return Behaviors.setup { context ->
                logger.info("created lgs master actor")
                context.system.receptionist().tell(Receptionist.register(serviceKey, context.self))
                Behaviors.withTimers { timers ->
                    Master(context, timers)
                }
            }
        }
    }

    private var fsRequestId = 0L
    private val chunkServers = HashMap<String, ChunkServerState>()
    private val chunkInstancedIds = HashMap<String, String>()
    private val chunkRefs = HashMap<String, ActorRef<ClusterProtocol>>()
    private val serviceKeys = java.util.HashMap<ServiceKey<ClusterProtocol>, ClusterProtocol.ChunkUp>();

    private val fs = FileSystem()
    private val chunkUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-chunk-up"), "cluster-chunk-up")
    private val masterUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawn(Topic.create(ClusterProtocol::class.java, "cluster-master-up"), "cluster-master-up")
    private val listingAdapter = context.messageAdapter(Receptionist.Listing::class.java) {
        ClusterProtocol.ListingRes(it)
    }
    private val reqIds = HashMap<Long, Long>()
    private val allocator = context.spawnAnonymous(AllocatorActor.create())
    private val instanceId = UUID.randomUUID().toString()

    init {
        val masterServiceRef = context.spawnAnonymous(MasterServiceActor.create(allocator, fs))
        // sends heartbeat to cluster ?
        timers.startTimerWithFixedDelay(
            MASTER_UP_TIMER_KEY,
            ClusterProtocol.MasterUP(Secrets.getSecrets().getHostName(), instanceId),
            Duration.ZERO,
            Duration.ofSeconds(5)
        )
        chunkUpTopic.tell(Topic.subscribe(context.self))
        val clusterMemberEventAdapter = context.messageAdapter(ClusterEvent.MemberEvent::class.java) {
            ClusterProtocol.ClusterMemberShipEvent(it)
        }
        val cluster = Cluster.get(context.system)
        cluster.subscriptions().tell(Subscribe.create(clusterMemberEventAdapter, ClusterEvent.MemberEvent::class.java))
        val masterApi = MasterServiceImpl(MasterApi(masterServiceRef, context.system))
        masterApi.startServer()
    }

    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder().onMessage(ClusterProtocol.MasterUP::class.java, this::onMasterUP)
            .onMessage(ClusterProtocol.ChunkUp::class.java, this::onChunkUp)
            .onMessage(ClusterProtocol.ListingRes::class.java, this::onListing)
            .onMessage(ClusterProtocol.ChunkInventory::class.java, this::onChunkInventory).build()
    }

    private fun onMasterUP(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        logger.debug("sending master up signal!")
        masterUpTopic.tell(Topic.publish(msg))
        return Behaviors.same()
    }

    private fun onListing(msg: ClusterProtocol.ListingRes): Behavior<ClusterProtocol> {
        serviceKeys.keys.stream().filter {
            logger.debug("chunk service key id: {}", it.id())
            it.id().equals(msg.listing.key.id())
        }.limit(1).forEach {
            val actors = msg.listing.getServiceInstances(it)
            if (actors.isNotEmpty()) {
                val chunkUpMsg = serviceKeys[msg.listing.key]!!
                chunkRefs[chunkUpMsg.serverHostName] = actors.first()
                logger.info(
                    "Received actor ref for ChunkServer: {}, key id: {}",
                    chunkRefs[chunkUpMsg.serverHostName]!!.path(), it.id()
                )
            }
        }
        return Behaviors.same()
    }

    private fun onChunkUp(msg: ClusterProtocol.ChunkUp): Behavior<ClusterProtocol> {
        if (!chunkRefs.containsKey(msg.serverHostName) || msg.instanceId != chunkInstancedIds[msg.serverHostName]) {
            logger.info("Received chunk up signal from : {}, retrieving actor ref", msg.serverHostName)
            val serviceKey = ServiceKey.create(ClusterProtocol::class.java, msg.serverHostName)
            serviceKeys[serviceKey] = msg
            context.system.receptionist().tell(Receptionist.find(serviceKey, listingAdapter))
            chunkInstancedIds[msg.serverHostName] = msg.instanceId
        } else {
            logger.info("Received chunk up signal from: {}", msg.serverHostName)
        }
        allocator.tell(AllocatorProtocol.UpdateServerState(msg.chunkServerState))
        return Behaviors.same()
    }

    private fun onChunkInventory(msg: ClusterProtocol.ChunkInventory): Behavior<ClusterProtocol> {
        logger.info("received chunk inventory from {}: {} chunk received", msg.serverHostName, msg.chunkIds.size)
        msg.chunkIds.forEach { chunkId ->
            fs.attachServerToChunk(msg.serverHostName, chunkId)
        }
        return Behaviors.same()
    }

    private fun onMemberEvent(msg: ClusterProtocol.ClusterMemberShipEvent): Behavior<ClusterProtocol> {
        when (val memberEvent = msg.event) {
            is ClusterEvent.MemberUp -> {
                memberEvent.member().address().host
            }
        }
        return Behaviors.same()
    }

    private fun getReqId(): Long {
        return 0L
    }
}