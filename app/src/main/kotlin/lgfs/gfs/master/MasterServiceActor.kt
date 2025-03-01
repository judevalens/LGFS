package lgfs.gfs.master

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.FileProtocol
import lgfs.gfs.FileSystem
import lgfs.gfs.allocator.AllocatorProtocol
import lgfs.network.ClusterProtocol
import org.slf4j.LoggerFactory

class MasterServiceActor(
	private val context: ActorContext<FileProtocol>,
	private val master: ActorRef<ClusterProtocol>,
	private val allocator: ActorRef<AllocatorProtocol>,
	private val fs: FileSystem
) : AbstractBehavior<FileProtocol>(context) {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)
	private val leaseGranReqs = HashMap<String, FileProtocol.LeaseGrantReq>()
	private val chunkRefs = java.util.HashMap<String, ActorRef<ClusterProtocol>>()

	companion object {
		fun create(
			master: ActorRef<ClusterProtocol>, allocator: ActorRef<AllocatorProtocol>, fs: FileSystem
		): Behavior<FileProtocol> {
			return Behaviors.setup {
				MasterServiceActor(it, master, allocator, fs)
			}
		}
	}

	override fun createReceive(): Receive<FileProtocol> {
		return newReceiveBuilder().onMessage(FileProtocol.CreateFileReq::class.java, this::onCreateFile)
			.onMessage(FileProtocol.CreateFileRes::class.java, this::onCreateFileRes)
			.onMessage(FileProtocol.DeleteFileReq::class.java, this::onDeleteFile)
			.onMessage(FileProtocol.LeaseGrantReq::class.java, this::onLeaseGrantReq)
			.onMessage(FileProtocol.LeaseGrantMapRes::class.java, this::onLeaseGrantRes)
			.build()
	}

	private val fileCreators = HashMap<String, ActorRef<FileCreatorActor.Command>>()

	private fun onCreateFile(msg: FileProtocol.CreateFileReq): Behavior<FileProtocol> {
		logger.info("spawning actor to create file, req id: {}", msg.reqId)
		fileCreators[msg.reqId] =
			context.spawn(FileCreatorActor.create(msg.reqId, msg.fileMetadata, fs, allocator, msg.replyTo), "fs")
		return Behaviors.same()
	}

	private fun onDeleteFile(msg: FileProtocol.DeleteFileReq): Behavior<FileProtocol> {
		logger.info("req id: {}, Processing file delete request for : {}", msg.reqId, msg.fileName)
		val isDeleted = fs.deleteFile(msg.fileName)
		msg.replyTo.tell(FileProtocol.DeleteFileRes(msg.reqId, msg.fileName, isDeleted))
		return Behaviors.same()
	}

	private fun onCreateFileRes(msg: FileProtocol.CreateFileRes): Behavior<FileProtocol> {
		logger.debug("req id: {} completed", msg.reqId)
		return Behaviors.same()
	}

	private fun onLeaseGrantReq(msg: FileProtocol.LeaseGrantReq): Behavior<FileProtocol> {
		val reqId = msg.reqId
		logger.info("req id : {}, Received lease grant request", msg.reqId)
		leaseGranReqs[reqId] = msg
		allocator.tell(AllocatorProtocol.LeaseGrantReq(reqId, msg.chunkMetadataList, context.self))
		return Behaviors.same()
	}

	private fun onLeaseGrantRes(msg: FileProtocol.LeaseGrantMapRes): Behavior<FileProtocol> {
		if (leaseGranReqs.containsKey(msg.reqId)) {
			val leaseGrantReq = leaseGranReqs[msg.reqId]!!
			val leaseGrantRes = FileProtocol.LeaseGrantRes(msg.reqId, ArrayList())
			// forwarding the leases to each primary chunk server
			msg.leases.forEach { (hostName, leases) ->
				leaseGrantRes.leases.addAll(leases)
				master.tell(
						ClusterProtocol.ForwardToChunkService(hostName, FileProtocol.LeaseGrantRes(msg.reqId, leases))
				)
			}
			//sending the leases back to the client
			leaseGrantReq.replyTo.tell(leaseGrantRes)
		}
		return Behaviors.same()
	}
}