package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.pattern.StatusReply
import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.network.JsonSerializable

class Allocator(context: ActorContext<Command>) : AbstractBehavior<Allocator.Command>(context) {
    interface Command
    class ChunkEntry @JsonCreator constructor(val handle: Long, val chunkIndex: Long) {
        var currentPath: String? = null
        val oldPaths: MutableList<String> = ArrayList()
    }

    data class ChunkInventoryList @JsonCreator constructor(val chunks: List<ChunkEntry>) : JsonSerializable

    class UpdateCurrentPath(
        val chunkHandle: Long,
        val path: String,
        val create: Boolean,
        val replyTo: ActorRef<Command>
    ) : Command

    class UpdateCurrentPathRes(val isSuccessful: Boolean) : Command
    class GetCurrentPath(val chunkHandle: Long, val replyTo: ActorRef<Command>) : Command
    class GetCurrentPathRes(val path: String?) : Command
    class GetChunkInventory(val replyTo: ActorRef<StatusReply<ChunkInventoryList>>) : Command
    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup {
                Allocator(it)
            }
        }
    }

    private val chunkEntries = HashMap<Long, ChunkEntry>()
    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(UpdateCurrentPath::class.java, this::onUpdateCurrentPath)
            .onMessage(GetCurrentPath::class.java, this::onGetCurrentPath)
            .onMessage(GetChunkInventory::class.java, this::onGetChunkInventory)
            .build()
    }

    private fun onGetCurrentPath(msg: GetCurrentPath): Behavior<Command> {
        if (!chunkEntries.containsKey(msg.chunkHandle)) {
            msg.replyTo.tell(GetCurrentPathRes(null))
            return Behaviors.same()
        }
        val chunkEntry = chunkEntries[msg.chunkHandle]!!
        msg.replyTo.tell(GetCurrentPathRes(chunkEntry.currentPath))
        return Behaviors.same()
    }

    private fun onUpdateCurrentPath(msg: UpdateCurrentPath): Behavior<Command> {
        if (!chunkEntries.containsKey(msg.chunkHandle)) {
            var isSuccessful = false
            if (msg.create) {
                val entry = ChunkEntry(msg.chunkHandle, -1)
                entry.currentPath = msg.path
                chunkEntries[msg.chunkHandle] = entry
                isSuccessful = true
            }
            msg.replyTo.tell(UpdateCurrentPathRes(isSuccessful))
            return Behaviors.same()
        }

        val entry = chunkEntries[msg.chunkHandle]!!
        if (entry.currentPath != null) {
            entry.oldPaths.add(entry.currentPath!!)
        }
        entry.currentPath = msg.path
        msg.replyTo.tell(UpdateCurrentPathRes(true))
        return Behaviors.same()
    }

    private fun onGetChunkInventory(msg: GetChunkInventory): Behavior<Command> {
        msg.replyTo.tell(StatusReply.success(ChunkInventoryList(chunkEntries.values.toList())))
        return Behaviors.same()
    }
}