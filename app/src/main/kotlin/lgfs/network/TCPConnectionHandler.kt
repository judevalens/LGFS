package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import java.net.Socket
import java.nio.ByteBuffer

class TCPConnectionHandler(
    context: ActorContext<State>,
    private val socket: Socket,
    private val replyTo: ActorRef<FileProtocol>
) {
    interface State

    companion object {
        fun create(socket: Socket, replyTo: ActorRef<FileProtocol>): Behavior<State> {
            return Behaviors.setup {
                TCPConnectionHandler(it, socket, replyTo).listening()
            }
        }
    }

    private fun listening(): Behavior<State> {
        val buffer = ByteArray(4)
        val payloadLenByte = socket.getInputStream().readNBytes(4)
        val payloadLen = ByteBuffer.allocate(4).put(payloadLenByte).getInt()
        val payloadId = socket.getInputStream().readNBytes(16)
        val payload = socket.getInputStream().readNBytes(payloadLen)
        replyTo.tell(ChunkService.PayloadData(payloadId, payload))
        return Behaviors.stopped()
    }

}