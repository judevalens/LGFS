package lgfs.network

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.ChunkData
import lgfs.gfs.Lease
import lgfs.gfs.MutationHolder

class ChunkService(context: ActorContext<FileProtocol>) : AbstractBehavior<FileProtocol>(context) {

    private val leases = HashMap<Long, Lease>()
    private val mutations = HashMap<Long, MutationHolder>()
    private val mutationData = HashMap<String, ChunkData>()

    override fun createReceive(): Receive<FileProtocol> {
        TODO("Not yet implemented")
    }

    private fun onChunkWriteReq(msg: FileProtocol.ChunkWriteReq): Behavior<FileProtocol> {
        return Behaviors.same()
    }
}