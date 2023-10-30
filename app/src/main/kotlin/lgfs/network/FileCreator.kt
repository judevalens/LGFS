package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.StashBuffer
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileSystem
import lgfs.gfs.StatManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class FileCreator(
    private val reqId: String,
    private val fileMetadata: FileMetadata,
    private val context: ActorContext<Command>,
    private val stashBuffer: StashBuffer<Command>,
    private val fs: FileSystem,
    private val statManager: ActorRef<StatManager.Command>,
    private val replyTo: ActorRef<FileProtocol>
) {
    interface Command
    private class CreateFile(
        val chunks: List<ChunkMetadata>
    ) : Command

    private class ChunkAllocationReq : Command
    private class ChunkAllocationRes(val chunks: List<ChunkMetadata>) : Command
    private class Exit : Command

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun create(
            reqId: String,
            fileMetadata: FileMetadata,
            fs: FileSystem,
            statManager: ActorRef<StatManager.Command>,
            replyTo: ActorRef<FileProtocol>
        ): Behavior<Command> {
            return Behaviors.withStash(1) { stash ->
                Behaviors.setup {
                    FileCreator(reqId, fileMetadata, it, stash, fs, statManager, replyTo).start()
                }
            }
        }
    }

    /**
     * Ask the [StatManager] where to put this file's chunk
     */
    private fun reqReplicas(): Behavior<Command> {
        return Behaviors.receive(Command::class.java).onMessage(ChunkAllocationReq::class.java) {
            context.ask(StatManager.ChunkAllocationRes::class.java, statManager, Duration.ofMinutes(1), {
                StatManager.ChunkAllocationReq(fileMetadata, it)
            }, { res, _ ->
                res?.let {
                    return@ask ChunkAllocationRes(it.replicationLocations)
                }
            })
            Behaviors.same()
        }.onMessage(ChunkAllocationRes::class.java, this::onChunkAllocationRes).build()
    }

    private fun start(): Behavior<Command> {
        logger.info("Requesting replicas location for new file! file path: {}, req id: $reqId", fileMetadata.path)
        stashBuffer.stash(ChunkAllocationReq())
        return stashBuffer.unstashAll(reqReplicas())
    }

    /**
     * Uses [FileSystem] to create the file and pass back the chunk location to the parent of this actor
     */
    private fun onChunkAllocationRes(msg: ChunkAllocationRes): Behavior<Command> {
        stashBuffer.stash(CreateFile(msg.chunks))
        return stashBuffer.unstashAll(onCreateFile())
    }

    private fun onCreateFile(): Behavior<Command> {
        return Behaviors.receive(Command::class.java)
            .onMessage(CreateFile::class.java) { msg2 ->
                logger.debug("adding file: ${fileMetadata.path} to directory tree, req id: $reqId")
                val fileCreated = fs.addFile(fileMetadata)
                if (fileCreated) {
                    replyTo.tell(FileProtocol.CreateFileRes(reqId, true, msg2.chunks))
                } else {
                    logger.info("Failed to add file path: ${fileMetadata.path} to directory tree, req id: $reqId")
                    replyTo.tell(FileProtocol.CreateFileRes(reqId, false, msg2.chunks))
                }
                Behaviors.stopped()
            }.build()

    }
}