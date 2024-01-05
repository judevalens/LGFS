package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import lgfs.gfs.FileProtocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.Socket
import java.nio.ByteBuffer

class TCPConnectionHandler(
    context: ActorContext<State>,
    private val socket: Socket,
    private val replyTo: ActorRef<FileProtocol>
) {
    interface State

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun create(socket: Socket, replyTo: ActorRef<FileProtocol>): Behavior<State> {
            return Behaviors.setup {
                TCPConnectionHandler(it, socket, replyTo).listening()
            }
        }
    }

    private fun listening(): Behavior<State> {
        val buffer = ByteArray(4)
        if (0 == 0) return Behaviors.stopped()
        val payloadLenByte = socket.getInputStream().readNBytes(4)
        val payloadLen = ByteBuffer.allocate(4).put(payloadLenByte).rewind().getInt()
        val payloadId = socket.getInputStream().readNBytes(16)
        val payload = socket.getInputStream().readNBytes(payloadLen)
        replyTo.tell(ChunkServiceActor.PayloadData(payloadId, payload))

        return Behaviors.stopped()
    }

}