package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import lgfs.gfs.FileSystem
import lgfs.gfs.StateManager
import org.slf4j.LoggerFactory

class MasterExecutor(
    private val context: ActorContext<FileProtocol>,
    private val statManager: ActorRef<StateManager.Command>,
    private val fs: FileSystem,
) : AbstractBehavior<FileProtocol>(context) {
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun create(
            statManager: ActorRef<StateManager.Command>,
            fs: FileSystem
        ): Behavior<FileProtocol> {
            return Behaviors.setup {
                MasterExecutor(it, statManager, fs)
            }
        }
    }

    override fun createReceive(): Receive<FileProtocol> {
        return newReceiveBuilder()
            .onMessage(FileProtocol.CreateFileReq::class.java, this::onCreateFile)
            .onMessage(FileProtocol.CreateFileRes::class.java, this::onCreateFileRes)
            .build()
    }

    private val fileCreators = HashMap<String, ActorRef<FileCreator.Command>>()

    private fun onCreateFile(msg: FileProtocol.CreateFileReq): Behavior<FileProtocol> {
        logger.info("spawning actor to create file, req id: {}", msg.reqId)
        fileCreators[msg.reqId] =
            context.spawn(FileCreator.create(msg.reqId, msg.fileMetadata, fs, statManager, msg.replyTo), "fs")

        return Behaviors.same()
    }

    private fun onCreateFileRes(msg: FileProtocol.CreateFileRes): Behavior<FileProtocol> {
        logger.debug("req id: {} completed", msg.reqId)
        return Behaviors.same()
    }
}