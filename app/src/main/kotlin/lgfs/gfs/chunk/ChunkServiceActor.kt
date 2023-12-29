package lgfs.gfs.chunk

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.FileProtocol
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket

class ChunkServiceActor(context: ActorContext<FileProtocol>) : AbstractBehavior<FileProtocol>(context) {
    private val chunkService = ChunkService()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    class HandleIncomingTCPConnection(val socket: Socket) : FileProtocol
    class PayloadData(val mutationId: ByteArray, val payload: ByteArray) : FileProtocol
    class LeaseGrant(val chunkHandle: Long, val givenAt: Long, val duration: Long) : FileProtocol

    companion object {
        fun create(): Behavior<FileProtocol> {
            return Behaviors.setup {
                ChunkServiceActor(it);
            }
        }
    }

    init {
        val server = ServerSocket(9005)
        val tcpThread = Thread {
            while (true) {
                logger.info(
                    "" +
                            "..........."
                )
                val incomingConnection = server.accept();
                context.self.tell(HandleIncomingTCPConnection(incomingConnection))
            }
        }
        tcpThread.start()
    }

    override fun createReceive(): Receive<FileProtocol> {
        return newReceiveBuilder()
            .onMessage(FileProtocol.Mutations::class.java, this::onMutations)
            .onMessage(FileProtocol.CommitMutation::class.java, this::onCommitMutation)
            .onMessage(LeaseGrant::class.java, this::onLeaseGrant)
            .onMessage(HandleIncomingTCPConnection::class.java, this::onHandleIncomingTCPConnection)
            .onMessage(PayloadData::class.java, this::onPayloadData)
            .build()
    }

    private fun onMutations(msg: FileProtocol.Mutations): Behavior<FileProtocol> {
        chunkService.addMutations(msg.mutations)
        return Behaviors.same()
    }

    private fun onCommitMutation(msg: FileProtocol.CommitMutation): Behavior<FileProtocol> {
        chunkService.commitMutation(msg.clientId, msg.chunkHandle, msg.replicas)
        return Behaviors.same()
    }

    private fun onHandleIncomingTCPConnection(msg: HandleIncomingTCPConnection): Behavior<FileProtocol> {
        context.spawnAnonymous(TCPConnectionHandler.create(msg.socket, context.self))
        return Behaviors.same()
    }

    private fun onPayloadData(msg: PayloadData): Behavior<FileProtocol> {
        chunkService.handlePayloadData(String(msg.mutationId), msg.payload)
        return Behaviors.same()
    }

    private fun onLeaseGrant(msg: LeaseGrant): Behavior<FileProtocol> {
        chunkService.handleLeaseGrant(msg.chunkHandle, msg.givenAt, msg.duration)
        return Behaviors.same()
    }
}