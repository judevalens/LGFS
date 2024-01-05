package lgfs.gfs.allocator

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.FileProtocol
import lgfs.network.ClusterProtocol
import org.slf4j.LoggerFactory

class AllocatorActor(context: ActorContext<AllocatorProtocol>, val master: ActorRef<ClusterProtocol>) :
    AbstractBehavior<AllocatorProtocol>(context) {
    private val allocator = Allocator()
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun create(master: ActorRef<ClusterProtocol>): Behavior<AllocatorProtocol> {
            return Behaviors.setup {
                AllocatorActor(it, master)
            }
        }
    }

    override fun createReceive(): Receive<AllocatorProtocol> {
        return newReceiveBuilder()
            .onMessage(AllocatorProtocol.ChunkAllocationReq::class.java, this::onAllocateChunks)
            .onMessage(AllocatorProtocol.UpdateServerState::class.java, this::onUpdateState)
            .onMessage(AllocatorProtocol.LeaseGrantReq::class.java, this::onLeaseGrantReq)
            .build()
    }

    private fun onAllocateChunks(msg: AllocatorProtocol.ChunkAllocationReq): Behavior<AllocatorProtocol> {
        val res = allocator.allocateChunks(msg.fileMetadata)
        msg.replyTo.tell(AllocatorProtocol.ChunkAllocationRes(res.first, res.second))
        return Behaviors.same()
    }

    /**
     * Updates the server queue by recomputing the new state's rank
     */
    private fun onUpdateState(msg: AllocatorProtocol.UpdateServerState): Behavior<AllocatorProtocol> {
        allocator.updateState(msg.state)
        return Behaviors.same()
    }

    private fun onLeaseGrantReq(msg: AllocatorProtocol.LeaseGrantReq): Behavior<AllocatorProtocol> {
        logger.info("{} - Processing lease grant request", msg.reqId)
        val leases = allocator.grantLease(msg.chunkMetadataList)
        msg.replyTo.tell(FileProtocol.LeaseGrantMapRes(msg.reqId,leases))

/*        leases.forEach { entry ->
            logger.debug("Forwarding lease grants to: ${entry.key}")
            master.tell(
                ClusterProtocol.ForwardToChunkService(
                    entry.key,
                    FileProtocol.LeaseGrantRes(msg.reqId, entry.value)
                )
            )
        }*/
        return Behaviors.same()
    }
}