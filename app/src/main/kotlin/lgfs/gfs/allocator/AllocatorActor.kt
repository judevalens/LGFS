package lgfs.gfs.allocator

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class AllocatorActor(context: ActorContext<AllocatorProtocol>) : AbstractBehavior<AllocatorProtocol>(context) {
    private val allocator = Allocator()

    companion object {
        fun create(): Behavior<AllocatorProtocol> {
            return Behaviors.setup {
                AllocatorActor(it)
            }
        }
    }

    override fun createReceive(): Receive<AllocatorProtocol> {
        return newReceiveBuilder()
            .onMessage(AllocatorProtocol.ChunkAllocationReq::class.java, this::onAllocateChunks)
            .onMessage(AllocatorProtocol.UpdateServerState::class.java, this::onUpdateState)
            .build()
    }

    private fun onAllocateChunks(msg: AllocatorProtocol.ChunkAllocationReq): Behavior<AllocatorProtocol> {
        val res = allocator.allocateChunks(msg.fileMetadata)
        msg.replyTo.tell(AllocatorProtocol.ChunkAllocationRes(res.first,res.second))
        return Behaviors.same()
    }

    /**
     * Updates the server queue by recomputing the new state's rank
     */
    private fun onUpdateState(msg: AllocatorProtocol.UpdateServerState): Behavior<AllocatorProtocol> {
        allocator.updateState(msg.state)
        return Behaviors.same()
    }
}