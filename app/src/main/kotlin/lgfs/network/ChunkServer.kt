package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.TimerScheduler
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.typed.Cluster
import lgfs.api.ChunkAPI
import lgfs.api.grpc.ChunkServiceImpl
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileProtocol
import lgfs.gfs.chunk.Allocator
import lgfs.gfs.chunk.ChunkServiceActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Optional
import java.util.UUID

class ChunkServer(context: ActorContext<ClusterProtocol>, timers: TimerScheduler<ClusterProtocol>) :
    AbstractBehavior<ClusterProtocol>(context) {
    class Ping : ClusterProtocol
    private class SendChunkUp : ClusterProtocol

    private val chunkUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawnAnonymous(Topic.create(ClusterProtocol::class.java, "cluster-chunk-up"))
    private val masterUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawnAnonymous(Topic.create(ClusterProtocol::class.java, "cluster-master-up"))
    private var isInitialized = false
    val cluster: Cluster = Cluster.get(context.system)
    private lateinit var masterRef: ActorRef<ClusterProtocol>
    private lateinit var masterInstanceId: String
    private val chunkServers = HashMap<ServerAddress, ActorRef<ClusterProtocol>>()
    private val serverInstanceIds = HashSet<String>()
    private val listingAdapter = context.messageAdapter(Receptionist.Listing::class.java) {
        ClusterProtocol.ListingRes(it)
    }
    private val instanceID = UUID.randomUUID().toString()
    private var masterServiceKey: Optional<ServiceKey<ClusterProtocol>> = Optional.empty()
    private val masterUpMsg: Optional<ClusterProtocol.MasterUP> = Optional.empty()
    private val chunkUpMessages = HashMap<ServiceKey<ClusterProtocol>, ClusterProtocol.ChunkUp>()
    private val chunkAllocator = context.spawnAnonymous(Allocator.create())
    private val chunkService: ActorRef<FileProtocol> = context.spawnAnonymous(ChunkServiceActor.create(chunkAllocator))

    init {
        masterUpTopic.tell(Topic.subscribe(context.self))
        chunkUpTopic.tell(Topic.subscribe(context.self))

        // Periodically send itself to other chunk servers via a pub-sub actor
        timers.startTimerWithFixedDelay(
            CHUNK_UP_TIMER_KEY, SendChunkUp(), Duration.ZERO, Duration.ofSeconds(5)
        )

        val chunkApi = ChunkAPI(chunkService, context.system)
        val chunkGrpcService = ChunkServiceImpl(chunkApi)
        chunkGrpcService.startServer()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val CHUNK_UP_TIMER_KEY = "chunk_up_timer"
        private val serviceKey = ServiceKey.create(ClusterProtocol::class.java, Secrets.getSecrets().getName())

        fun create(): Behavior<ClusterProtocol> {
            return Behaviors.setup { context ->
                context.system.receptionist().tell(Receptionist.register(serviceKey, context.self))
                Behaviors.withTimers { timer ->
                    logger.info("created lgs chunk actor")
                    ChunkServer(context, timer)
                }
            }
        }
    }

    override fun createReceive(): Receive<ClusterProtocol> {
        return newReceiveBuilder().onMessage(SendChunkUp::class.java) { this.onSendChunkUp() }
            .onMessage(ClusterProtocol.MasterUP::class.java, this::handleMasterUp)
            .onMessage(ClusterProtocol.RequestChunkInventory::class.java) { this.onRequestChunkInventory() }
            .onMessage(ClusterProtocol.ChunkUp::class.java, this::handleChunkUp)
            .onMessage(ClusterProtocol.ListingRes::class.java, this::onListing)
            .onMessage(ClusterProtocol.ChunkInventory::class.java, this::onChunkInventory)
            .onMessage(ClusterProtocol.ForwardToChunkService::class.java, this::onForwardToChunkService).build()
    }

    private fun handleMasterUp(msg: ClusterProtocol.MasterUP): Behavior<ClusterProtocol> {
        if (!this::masterRef.isInitialized || !this::masterInstanceId.isInitialized || msg.instanceId != masterInstanceId) {
            logger.info("Received master up signal from : {}, retrieving actor ref", msg.serverAddress)
            masterServiceKey = Optional.of(ServiceKey.create(ClusterProtocol::class.java, msg.serverAddress.hostName))
            context.system.receptionist().tell(Receptionist.find(masterServiceKey.get(), listingAdapter))
            masterInstanceId = msg.instanceId
        } else {
            //logger.info("Received master up signal from : {}", msg.serverAddress)
        }
        return Behaviors.same()
    }

    private fun handleChunkUp(msg: ClusterProtocol.ChunkUp): Behavior<ClusterProtocol> {

        if (serverInstanceIds.contains(msg.instanceId) || msg.serverAddress == Secrets.getSecrets()
                .getServerAddress()
        ) {
            return Behaviors.same()
        }
        logger.info("Received chunk up signal, retrieving actor ref at : {}", msg.serverAddress)
        val serviceKey = ServiceKey.create(ClusterProtocol::class.java, msg.serverAddress.hostName)
        context.system.receptionist().tell(Receptionist.find(serviceKey, listingAdapter))
        chunkUpMessages[serviceKey] = msg
        serverInstanceIds.add(msg.instanceId)
        return Behaviors.same()
    }

    private fun onListing(msg: ClusterProtocol.ListingRes): Behavior<ClusterProtocol> {
        if (!masterServiceKey.isEmpty && msg.listing.key.id().equals(masterServiceKey.get().id())) {
            logger.debug("master service key: {}", msg.listing.key.id())
            val refs = msg.listing.getServiceInstances(masterServiceKey.get())
            if (refs.isEmpty()) {
                logger.debug("listing for master ref is empty")
                return Behaviors.same()
            }
            masterRef = refs.first()
            isInitialized = true
            logger.info("Received actor ref for Master: {}", masterRef.path().address())
        } else {
            chunkUpMessages.keys.stream().filter {
                logger.debug("chunk service key id: {}", it.id())
                it.id().equals(msg.listing.key.id())
            }.limit(1).forEach {
                val actors = msg.listing.getServiceInstances(it)
                if (actors.isNotEmpty()) {
                    val chunkUpMsg = chunkUpMessages[msg.listing.key]!!
                    chunkServers[chunkUpMsg.serverAddress] = actors.first()
                    logger.info(
                        "Received actor ref for ChunkServer: {}, key id: {}",
                        chunkServers[chunkUpMsg.serverAddress]!!.path(),
                        it.id()
                    )
                }
            }
        }
        return Behaviors.same()
    }

    /**
     * Sends chunk heartbeat signal
     */
    private fun onSendChunkUp(): Behavior<ClusterProtocol> {
        //logger.info("sending chunk up msg: ${context.self.path()}")
        chunkUpTopic.tell(
            Topic.publish(
                ClusterProtocol.ChunkUp(
                    Secrets.getSecrets().getServerAddress(), getState(), instanceID
                )
            )
        )
        return Behaviors.same()
    }

    /**
     * Requests chunks inventory from [Allocator]
     */
    private fun onRequestChunkInventory(): Behavior<ClusterProtocol> {
        if (!isInitialized) return Behaviors.same()
        logger.info("Requesting chunks inventory from Chunk Allocator actor")
        context.askWithStatus(Allocator.ChunkInventoryList::class.java,
            chunkAllocator,
            Duration.ofSeconds(1000),
            { Allocator.GetChunkInventory(it) },
            { chunkList, _ ->
                ClusterProtocol.ChunkInventory(Secrets.getSecrets().getHostName(), chunkList)
            })
        return Behaviors.same()
    }

    /**
     * Sends the chunk list received from [Allocator] to [masterRef]
     */
    private fun onChunkInventory(msg: ClusterProtocol.ChunkInventory): Behavior<ClusterProtocol> {
        masterRef.tell(msg)
        return Behaviors.same()
    }

    private fun onForwardToChunkService(msg: ClusterProtocol.ForwardToChunkService): Behavior<ClusterProtocol> {
        logger.info("forwarding message to chunk service")
        chunkService.tell(msg.fileProtocolMsg)
        return Behaviors.same()
    }

    private fun getState(): ChunkServerState {
        return ChunkServerState(Secrets.getSecrets().getServerAddress())
    }
}