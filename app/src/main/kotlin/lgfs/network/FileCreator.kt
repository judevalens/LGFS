package lgfs.network

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.StashBuffer
import lgfs.api.MasterAPI
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileSystem
import lgfs.gfs.StatManager
import java.time.Duration

class FileCreator(
    private val reqId: String,
    private val fileMetadata: FileMetadata,
    private val context: ActorContext<Command>,
    private val stashBuffer: StashBuffer<Command>,
    private val fs: FileSystem,
    private val statManager: ActorRef<StatManager.Command>,
    private val replyTo: ActorRef<Master.CreateFileRes>
) {
    interface Command
    private class CreateFile(
        val replicas: List<Pair<String, Set<String>>>
    ) : Command

    private class ReplicaLocationsReq : Command
    private class ReplicaLocationsRes(val replicas: List<Pair<String, Set<String>>>) : Command
    private class Exit : Command

    companion object {
        fun create(
            reqId: String,
            fileMetadata: FileMetadata,
            fs: FileSystem,
            statManager: ActorRef<StatManager.Command>,
            replyTo: ActorRef<Master.CreateFileRes>
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
                val fileCreated = fs.addFile(fileMetadata)
                if (fileCreated) {
                    context.ask(
                        ClusterProtocol.NoOp::class.java,
                        replyTo, Duration.ofMinutes(100),
                        {
                            Master.CreateFileRes(reqId, true, msg2.replicas)
                        },
                        { _, _ ->
                            Exit()
                        }
                    )
                } else {
                    TODO()
                }
                Behaviors.same()
            }
            .onMessage(Exit::class.java) {
                Behaviors.stopped()
            }.build()

    }
}