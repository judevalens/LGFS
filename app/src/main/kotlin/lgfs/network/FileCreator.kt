package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.StashBuffer
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
        val replicas: List<Pair<String, Set<String>>>
    ) : Command

    private class ReplicaLocationsReq : Command
    private class ReplicaLocationsRes(val replicas: List<Pair<String, Set<String>>>) : Command
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
        return Behaviors.receive(Command::class.java).onMessage(ReplicaLocationsReq::class.java) {
            context.ask(StatManager.ReplicaLocationsRes::class.java, statManager, Duration.ofMinutes(1), {
                StatManager.ReplicaLocationsReq(fileMetadata, it)
            }, { res, _ ->
                res?.let {
                    return@ask ReplicaLocationsRes(it.replicationLocations)
                }
            })
            Behaviors.same()
        }.onMessage(ReplicaLocationsRes::class.java, this::onReplicaRes).build()
    }

    private fun start(): Behavior<Command> {
        logger.info("Requesting replicas location for new file! file path: {}, req id: $reqId", fileMetadata.path)
        stashBuffer.stash(ReplicaLocationsReq())
        return stashBuffer.unstashAll(reqReplicas())
    }

    /**
     * Uses [FileSystem] to create the file and pass back the chunk location to the parent of this actor
     */
    private fun onReplicaRes(msg: ReplicaLocationsRes): Behavior<Command> {
        stashBuffer.stash(CreateFile(msg.replicas))
        return stashBuffer.unstashAll(onCreateFile())
    }

    private fun onCreateFile(): Behavior<Command> {
        return Behaviors.receive(Command::class.java)
            .onMessage(CreateFile::class.java) { msg2 ->
                logger.debug("adding file: ${fileMetadata.path} to directory tree, req id: $reqId")
                val fileCreated = fs.addFile(fileMetadata)
                if (fileCreated) {
                    context.ask(
                        ClusterProtocol.NoOp::class.java,
                        replyTo, Duration.ofMinutes(100),
                        {
                            FileProtocol.CreateFileRes(reqId, true, msg2.replicas)
                        },
                        { _, _ ->
                            logger.debug("Done process file creation request: $reqId, exiting...")
                            Exit()
                        }
                    )
                } else {
                    logger.info("Failed to add file path: ${fileMetadata.path} to directory tree, req id: $reqId")
                    FileProtocol.CreateFileRes(reqId, false, emptyList())
                }
                Behaviors.same()
            }
            .onMessage(Exit::class.java) {
                Behaviors.stopped()
            }.build()

    }
}