package lgfs.gfs

import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive

class TCPConnectionHandler(context: ActorContext<Unit>) : AbstractBehavior<Unit>(context){
    override fun createReceive(): Receive<Unit> {
        TODO("Not yet implemented")
    }
}