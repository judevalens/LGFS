package lgfs.client

import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Receive
import java.nio.file.Files
import java.nio.file.Path

class Client(context: ActorContext<Command>?) : AbstractBehavior<Client.Command>(context) {
    interface Command

    override fun createReceive(): Receive<Command> {
        TODO("Not yet implemented")
    }
gi
    fun createFile(filePathStr: String) {
        val filePath = Path.of(filePathStr)
        if (Files.exists(filePath)) {

        }

    }
}