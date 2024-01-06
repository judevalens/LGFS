package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.typed.Cluster
import lgfs.api.ChunkAPI
import lgfs.api.grpc.ChunkServiceImpl
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.ChunkServerState
import lgfs.gfs.FileProtocol
import lgfs.gfs.chunk.ChunkServiceActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class ChunkServer(context: ActorContext<ClusterProtocol>, timers: TimerScheduler<ClusterProtocol>) :
    AbstractBehavior<ClusterProtocol>(context) {
    class Ping() : ClusterProtocol
    private class SendChunkUp : ClusterProtocol

    private val chunkUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawnAnonymous(Topic.create(ClusterProtocol::class.java, "cluster-chunk-up"))
    private val masterUpTopic: ActorRef<Topic.Command<ClusterProtocol>> =
        context.spawnAnonymous(Topic.create(ClusterProtocol::class.java, "cluster-master-up"))
    private var isInitialized = false
    val cluster: Cluster = Cluster.get(context.system)
    private lateinit var masterRef: ActorRef<ClusterProtocol>
    private lateinit var masterInstanceId: String
    private val chunks = HashMap<Long, ChunkMetadata>()
    private val chunkServers = HashMap<ServerAddress, ActorRef<ClusterProtocol>>()
    private val listingAdapter = context.messageAdapter(Receptionist.Listing::class.java) {
        ClusterProtocol.ListingRes(it)
    }
    private val instanceID = UUID.randomUUID().toString()
    private var masterServiceKey: Optional<ServiceKey<ClusterProtocol>> = Optional.empty()
    private val masterUpMsg: Optional<ClusterProtocol.MasterUP> = Optional.empty()
    private val serviceKeys = HashMap<ServiceKey<ClusterProtocol>, ClusterProtocol.ChunkUp>();
    private val chunkService: ActorRef<FileProtocol> = context.spawnAnonymous(ChunkServiceActor.create())

    init {
        masterUpTopic.tell(Topic.subscribe(context.self))
        chunkUpTopic.tell(Topic.subscribe(context.self))

        // Periodically send itself to other chunk servers via a pub-sub actor
        timers.startTimerWithFixedDelay(
            CHUNK_UP_TIMER_KEY,
            SendChunkUp(),
            Duration.ZERO,
            Duration.ofSeconds(5)
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
                context
                    .system
                    .receptionist()
                    .tell(Receptionist.register(serviceKey, context.self));
                Behaviors.withTimers { timer ->
                    logger.info("created lgs chunk actor")
                    ChunkServer(context, timer)
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
            .onMessage(ClusterProtocol.ListingRes::class.java, this::onListing)
            .onMessage(ClusterProtocol.ForwardToChunkService::class.java, this::onForwardToChunkService)
            .build()
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

        if (msg.serverAddress == Secrets.getSecrets().getServerAddress()) {
            logger.debug("Received own chunk up signal, retrieving actor ref at : {}", msg.serverAddress)
            return Behaviors.same()
        }

        logger.info("Received chunk up signal, retrieving actor ref at : {}", msg.serverAddress)
        val serviceKey = ServiceKey.create(ClusterProtocol::class.java, msg.serverAddress.hostName)
        context.system.receptionist()
            .tell(Receptionist.find(serviceKey, listingAdapter))
        serviceKeys[serviceKey] = msg
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

            serviceKeys.keys.stream().filter {
                logger.debug("chunk service key id: {}", it.id())
                it.id().equals(msg.listing.key.id())
            }.limit(1).forEach {
                val actors = msg.listing.getServiceInstances(it)
                if (actors.isNotEmpty()) {
                    val chunkUpMsg = serviceKeys[msg.listing.key]!!
                    chunkServers[chunkUpMsg.serverAddress] = actors.first()
                    logger.info(
                        "Received actor ref for ChunkServer: {}, key id: {}",
                        chunkServers[chunkUpMsg.serverAddress]!!.path(), it.id()
                    )
                }
            }
        }
        return Behaviors.same()
    }

    /**
     *
     */
    private fun onSendChunkUp(msg: SendChunkUp): Behavior<ClusterProtocol> {
        //logger.info("sending chunk up msg: ${context.self.path()}")
        chunkUpTopic.tell(
            Topic.publish(
                ClusterProtocol.ChunkUp(
                    Secrets.getSecrets().getServerAddress(),
                    getState(),
                    instanceID
                )
            )
        )
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

    private fun onForwardToChunkService(msg: ClusterProtocol.ForwardToChunkService): Behavior<ClusterProtocol> {
        logger.info("forwarding message to chunk service")
        chunkService.tell(msg.fileProtocolMsg);
        return Behaviors.same()
    }

    private fun getState(): ChunkServerState {
        return ChunkServerState(Secrets.getSecrets().getServerAddress())
    }
}