package lgfs.gfs

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import lgfs.gfs.chunk.Allocator
import lgfs.gfs.chunk.ChunkService
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CompletionStage


class ChunkFile(
	private val chunkHandle: Long, private val chunkAllocator: ActorRef<Allocator.Command>, private val system: ActorSystem<Void>
) {
	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	class Memento(val file: File)

	private var isInTransaction = false
	private var tmpFile: File? = null
	private var currentPath: Path
	var version = 0

	init {
		runBlocking {
			logger.info("Initializing chunk file | chunk handle: {}", chunkHandle)
			val resFuture: CompletionStage<Allocator.Command> = AskPattern.ask(
				chunkAllocator, {
					Allocator.GetCurrentPath(chunkHandle, it)
				},
				Duration.ofSeconds(100000),
				system.scheduler()
			)
			val res = resFuture.await() as Allocator.GetCurrentPathRes

			if (res.path != null) {
				currentPath = Paths.get(res.path)
			} else {
				val chunkPath = getChunkPath(chunkHandle)
				val resFuture: CompletionStage<Allocator.Command> = AskPattern.ask(
					chunkAllocator, {
						Allocator.UpdateCurrentPath(chunkHandle, chunkPath.toString(), true, it)
					},
					Duration.ofSeconds(100000),
					system.scheduler()
				)
				val res = resFuture.await() as Allocator.UpdateCurrentPathRes
				if (res.isSuccessful) {
					currentPath = chunkPath
				} else {
					throw IOException("Failed to create chunk file")
				}
			}
			logger.info("Initialized chunk file with handle: {} at path: {}", chunkHandle, currentPath.toString())
		}
	}

	fun writeChunk(mutation: FileProtocol.Mutation, chunkData: ChunkData): Boolean {
		if (!isInTransaction) throw Exception(IllegalStateException())
		tmpFile!!.outputStream().write(chunkData.payload, mutation.offset, chunkData.payload.size)
		return true
	}

	fun startTransaction() {
		if (isInTransaction) throw Exception(IllegalStateException())
		isInTransaction = true
		version++
		tmpFile = getChunkPath(chunkHandle).toFile()
	}

	fun commitTransaction(): Boolean {
		if (!isInTransaction) throw Exception(IllegalStateException())
		isInTransaction = false
		logger.info("Send add mutation msg to chunkService actor")

		val res = runBlocking {
			val resFuture: CompletionStage<Allocator.Command> = AskPattern.ask(
				chunkAllocator, {
					Allocator.UpdateCurrentPath(chunkHandle, tmpFile!!.path, false, it)
				}, Duration.ofSeconds(100000), system.scheduler()
			)
			resFuture.await()
		} as Allocator.UpdateCurrentPathRes

		if (res.isSuccessful) {
			currentPath = tmpFile!!.toPath()
		}
		tmpFile = null
		return res.isSuccessful
	}

	fun getFilePath(): Path {
		return currentPath
	}

	fun getChunkPath(chunkHandle: Long): Path {
		return Paths.get(ChunkService.ROOT_CHUNK_PATH.toString(), "${ChunkService.hexHandle(chunkHandle)}-${version}")
	}


}