package lgfs.gfs.master

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.StashBuffer
import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileProtocol
import lgfs.gfs.FileSystem
import lgfs.gfs.allocator.AllocatorProtocol
import lgfs.gfs.allocator.AllocatorActor

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class FileCreatorActor(
    private val reqId: String,
    private val fileMetadata: FileMetadata,
    private val context: ActorContext<Command>,
    private val stashBuffer: StashBuffer<Command>,
    private val fs: FileSystem,
    private val allocator: ActorRef<AllocatorProtocol>,
    private val replyTo: ActorRef<FileProtocol>
) {
    interface Command
    private class CreateFile(
        val chunks: List<ChunkMetadata>?
    ) : Command

    private class ChunkAllocationReq : Command
    private class ChunkAllocationRes(val isSuccessful: Boolean, val chunks: List<ChunkMetadata>?) : Command

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun create(
            reqId: String,
            fileMetadata: FileMetadata,
            fs: FileSystem,
            allocator: ActorRef<AllocatorProtocol>,
            replyTo: ActorRef<FileProtocol>
        ): Behavior<Command> {
            return Behaviors.withStash(1) { stash ->
                Behaviors.setup {
                    FileCreatorActor(reqId, fileMetadata, it, stash, fs, allocator, replyTo).start()
                }
            }
        }
    }

    /**
     * Ask the [AllocatorActor] where to put this file's chunk
     */
    private fun createChunks(): Behavior<Command> {
        return Behaviors.receive(Command::class.java).onMessage(ChunkAllocationReq::class.java) { _ ->
            context.ask(AllocatorProtocol.ChunkAllocationRes::class.java, allocator, Duration.ofMinutes(1), {
                logger.info(
                    "Requesting replicas location for new file! file path: {}, req id: $reqId", fileMetadata.path
                )
                AllocatorProtocol.ChunkAllocationReq(fileMetadata, it)
            }, { res, err ->
                if (res != null) {
                    return@ask ChunkAllocationRes(res.isSuccessful, res.replicationLocations)
                } else {
                    logger.error("{} - Failed to allocate chunk \n {}", reqId, err.message)
                    return@ask ChunkAllocationRes(false, null)
                }
            })
            Behaviors.same()
        }.onMessage(ChunkAllocationRes::class.java, this::onChunkAllocationRes).build()
    }

    private fun start(): Behavior<Command> {
        logger.info(
            "{} - Creating file at path : {}, size: {}, isDir: {}",
            reqId,
            fileMetadata.path,
            fileMetadata.size,
            fileMetadata.isDir
        )
        stashBuffer.stash(ChunkAllocationReq())
        return stashBuffer.unstashAll(createChunks())
    }

    /**
     * Uses [FileSystem] to create the file and pass back the chunk location to the parent of this actor
     */
    private fun onChunkAllocationRes(msg: ChunkAllocationRes): Behavior<Command> {
        stashBuffer.stash(CreateFile(msg.chunks))
        return stashBuffer.unstashAll(onCreateFile())
    }

    private fun onCreateFile(): Behavior<Command> {
        return Behaviors.receive(Command::class.java).onMessage(CreateFile::class.java) { msg ->
                logger.info("{} - creating file : {} in FS", reqId, fileMetadata.path)
                if (msg.chunks?.isEmpty() == true) {
                    logger.info("{} - failed to allocate chunks for file : {}", reqId, fileMetadata.path)
                    replyTo.tell(FileProtocol.CreateFileRes(reqId, false, emptyList()))
                    return@onMessage Behaviors.stopped()
                }
                val fileCreated = fs.addFile(fileMetadata)
                if (fileCreated) {
                    logger.info("{} - Created file at: {}", reqId, fileMetadata.path)
                    replyTo.tell(FileProtocol.CreateFileRes(reqId, true, msg.chunks))
                } else {
                    logger.info("{} - Failed to add file path: {} to directory tree", reqId, fileMetadata.path)
                    replyTo.tell(FileProtocol.CreateFileRes(reqId, false, emptyList()))
                }
                Behaviors.stopped()
            }.build()

    }
}