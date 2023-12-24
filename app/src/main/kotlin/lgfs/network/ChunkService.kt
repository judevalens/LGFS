package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.ChunkData
import lgfs.gfs.Lease
import lgfs.gfs.MutationHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket

class ChunkService(context: ActorContext<FileProtocol>) : AbstractBehavior<FileProtocol>(context) {
    private val leases = HashMap<Long, Lease>()
    private val mutations = HashMap<Long, MutationHolder>()
    private val mutationData = HashMap<String, ChunkData>()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    class SendIncomingConnection(val socket: Socket) : FileProtocol
    class IncomingConnection(val socket: Socket, val replyTo: ActorRef<FileProtocol>) : FileProtocol
    class PayloadData(val mutationId: ByteArray, val payload: ByteArray) : FileProtocol
    class LeaseGrant(val chunkHandle: Long, val givenAt: Long, val duration: Long) : FileProtocol


    companion object {
        fun create(): Behavior<FileProtocol> {
            return Behaviors.setup {
                ChunkService(it);
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
                context.self.tell(SendIncomingConnection(incomingConnection))
            }
        }
        tcpThread.start()
    }

    override fun createReceive(): Receive<FileProtocol> {
        return newReceiveBuilder()
            .onMessage(FileProtocol.Mutations::class.java, this::onMutations)
            .onMessage(FileProtocol.CommitMutation::class.java, this::onCommitMutation)
            .onMessage(LeaseGrant::class.java, this::onLeaseGrant)
            .onMessage(SendIncomingConnection::class.java, this::onSendIncomingConnection)
            .onMessage(PayloadData::class.java, this::onPayloadData)
            .build()
    }

    private fun onMutations(msg: FileProtocol.Mutations): Behavior<FileProtocol> {
        val mutationHolder =
            msg.mutations.forEach { mutation ->
                var mutationHolder = MutationHolder(mutation.chunkHandle)
                if (mutations.containsKey(mutation.chunkHandle)) {
                    mutationHolder = mutations[mutation.chunkHandle]!!
                }
                mutationHolder.addMutation(mutation, isPrimary(mutation.primary))

            }
        return Behaviors.same()
    }

    private fun isPrimary(hostname: String): Boolean {
        return TODO()
    }

    private fun onCommitMutation(msg: FileProtocol.CommitMutation): Behavior<FileProtocol> {
        if (mutations.containsKey(msg.chunkHandle)) {
            mutations[msg.chunkHandle]!!.commitMutation(msg.clientId, TODO())
        }
        return Behaviors.same()
    }

    private fun onChunkWriteReq(msg: FileProtocol.ChunkWriteReq): Behavior<FileProtocol> {
        return Behaviors.same()
    }

    private fun onSendIncomingConnection(msg: SendIncomingConnection): Behavior<FileProtocol> {
        context.spawnAnonymous(TCPConnectionHandler.create(msg.socket, context.self))
        return Behaviors.same()
    }

    private fun onPayloadData(msg: PayloadData): Behavior<FileProtocol> {
        val mutationId = String(msg.mutationId)
        mutationData[mutationId] = ChunkData(0, "", msg.payload)
        return Behaviors.same()
    }

    private fun onLeaseGrant(msg: LeaseGrant): Behavior<FileProtocol> {
        val lease = Lease(msg.chunkHandle, msg.givenAt)
        leases[msg.chunkHandle] = lease
        return Behaviors.same()
    }
}