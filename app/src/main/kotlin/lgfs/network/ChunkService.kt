package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.*
import lgfs.gfs.ChunkData
import lgfs.gfs.Lease
import lgfs.gfs.Mutation
import lgfs.gfs.MutationHolder
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration

class ChunkService(context: ActorContext<FileProtocol>) : AbstractBehavior<FileProtocol>(context) {

    private val leases = HashMap<Long, Lease>()
    private val mutations = HashMap<Long, MutationHolder>()
    private val mutationData = HashMap<String, ChunkData>()

    class SendIncomingConnection(val socket: Socket) : FileProtocol
    class IncomingConnection(val socket: Socket, val replyTo: ActorRef<FileProtocol>) : FileProtocol
    class PayloadData(val payloadId: ByteArray, val payload: ByteArray) : FileProtocol
    class LeaseGrant(val chunkHandle: Long, val givenAt: Long, val duration: Long) : FileProtocol
    class Mutation(val clientId: String, val chunkHandle: Long, val payloadId: String) : FileProtocol
    class CommitMutation(val clientId: String, val chunkHandle: Long) : FileProtocol

    init {
        val server = ServerSocket()
        val tcpThread = Thread {
            while (true) {
                val incomingConnection = server.accept();
                AskPattern.ask(
                    context.self,
                    { _: ActorRef<FileProtocol> ->
                        SendIncomingConnection(incomingConnection)
                    },
                    Duration.ofMinutes(10000),
                    context.system.scheduler()
                )

            }
        }
        tcpThread.start()
    }

    override fun createReceive(): Receive<FileProtocol> {
        TODO("Not yet implemented")
    }

    private fun onMutation(msg: Mutation): Behavior<FileProtocol> {
        if (!mutations.containsKey(msg.chunkHandle)) {
            mutations[msg.chunkHandle] = MutationHolder(0)
        }
        mutations[msg.chunkHandle]!!.addMutation(msg.clientId, msg.payloadId,0)
        return Behaviors.same()
    }

    private fun onCommitMutation(msg: CommitMutation): Behavior<FileProtocol> {
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
        val payloadId = String(msg.payloadId)
        mutationData[payloadId] = ChunkData(0, "", msg.payload)
        return Behaviors.same()

    }

    private fun onLeaseGrant(msg: LeaseGrant): Behavior<FileProtocol> {

        val lease = Lease(msg.chunkHandle, msg.givenAt)
        leases[msg.chunkHandle] = lease

        return Behaviors.same()
    }
}