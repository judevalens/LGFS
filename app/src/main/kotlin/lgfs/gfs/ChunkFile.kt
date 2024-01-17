package lgfs.gfs

import lgfs.gfs.chunk.ChunkService
import lgfs.network.Secrets
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.file.Paths


class ChunkFile(private val chunkHandle: String) {

	private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

	class Memento(val file: File)

	private var isInTransaction = false
	private var tmpFile: File? = null
	var version = 0

	fun writeChunk(mutation: FileProtocol.Mutation, chunkData: ChunkData): Boolean {
		if ((isInTransaction)) throw Exception(IllegalStateException())
		tmpFile!!.outputStream().write(chunkData.payload, mutation.offset, chunkData.payload.size)
		return true
	}

	fun startTransaction() {
		if (isInTransaction) throw Exception(IllegalStateException())
		isInTransaction = true
		tmpFile = kotlin.io.path.createTempFile(directory = ChunkService.ROOT_CHUNK_PATH).toFile()
	}

	fun commitTransaction() {
		if (!isInTransaction) throw Exception(IllegalStateException())
		isInTransaction = false
	}
}