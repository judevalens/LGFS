package lgfs.network

import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive

class Client(context: ActorContext<ClusterProtocol>) : AbstractBehavior<ClusterProtocol>(context) {
    class CreateFile(val filePath: String)

    override fun createReceive(): Receive<ClusterProtocol> {
        TODO("Not yet implemented")
    }

    private fun onCreateFile(msg: CreateFile) {
    }

    private fun sendCreateFileRequest() {

    }

}