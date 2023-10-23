package lgfs.api

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.FileMetadata
import lgfs.network.ClusterProtocol
import lgfs.network.Master
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class MasterImplementation(context: ActorContext<Command>, private val masterActor: ActorRef<ClusterProtocol>) :
    AbstractBehavior<MasterImplementation.Command>(context), MasterAPI {
    interface Command

    class CreateFileRes(val reqId: String, val successful: Boolean, val replicas: List<Pair<String, Set<String>>>) :
        Command

    private val fileCreationActor: ActorRef<Master.CreateFileRes>
    private val pendingRequests = HashMap<String, CompletableFuture<Boolean>>()

    init {
        fileCreationActor = context.messageAdapter(Master.CreateFileRes::class.java) { msg ->
            CreateFileRes(msg.reqId, msg.successful, msg.replicas)
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(CreateFileRes::class.java, this::onCreateFileRes)
            .build()
    }

    override fun createFile(fileMetadata: FileMetadata): Future<Boolean> {
        val reqId = UUID.randomUUID().toString()
        masterActor.tell(Master.CreateFileReq(reqId, fileMetadata, fileCreationActor))
        val future = CompletableFuture<Boolean>();
        pendingRequests[reqId] = future
        return future
    }

    private fun onCreateFileRes(msg: CreateFileRes): Behavior<Command> {
        if (pendingRequests.containsKey(msg.reqId)) {
            pendingRequests[msg.reqId]!!.complete(msg.successful)
        } else {
            TODO("must handle rogue response")
        }
        return Behaviors.same()
    }

    private fun getReqId(): Long {
        return 0L
    }
}