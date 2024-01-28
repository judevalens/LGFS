package lgfs.gfs.chunk

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Allocator(context: ActorContext<Command>) : AbstractBehavior<Allocator.Command>(context) {
	interface Command {}
	class ChunkEntry(val hunkHandle: Long) {
		var currentPath: String? = null
		val oldPaths: MutableList<String> = ArrayList()
	}

	class UpdateCurrentPath(val chunkHandle: Long, val path: String, val create: Boolean, val replyTo: ActorRef<Command>) :
		Command

	class UpdateCurrentPathRes(val isSuccessful: Boolean) : Command
	class GetCurrentPath(val chunkHandle: Long, val replyTo: ActorRef<Command>) : Command
	class GetCurrentPathRes(val path: String?) : Command

	companion object {
		fun create(): Behavior<Command> {
			return Behaviors.setup {
				Allocator(it);
			}
		}
	}

	private val chunkEntries = HashMap<Long, ChunkEntry>()
	override fun createReceive(): Receive<Command> {
		return newReceiveBuilder()
			.onMessage(UpdateCurrentPath::class.java, this::onUpdateCurrentPath)
			.onMessage(GetCurrentPath::class.java, this::onGetCurrentPath)
			.build()
	}

	private fun onGetCurrentPath(msg: GetCurrentPath): Behavior<Command> {
		if (!chunkEntries.containsKey(msg.chunkHandle)) {
			msg.replyTo.tell(GetCurrentPathRes(null));
			return Behaviors.same()
		}
		val chunkEntry = chunkEntries[msg.chunkHandle]!!
		msg.replyTo.tell(GetCurrentPathRes(chunkEntry.currentPath));
		return Behaviors.same()
	}

	private fun onUpdateCurrentPath(msg: UpdateCurrentPath): Behavior<Command> {
		if (!chunkEntries.containsKey(msg.chunkHandle)) {
			var isSuccessful = false
			if (msg.create) {
				val entry = ChunkEntry(msg.chunkHandle)
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
}