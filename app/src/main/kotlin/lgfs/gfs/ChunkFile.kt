package lgfs.gfs

import lgfs.network.Secrets
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths


class ChunkFile(private val chunkHandle: String) {
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    class Memento(val file: File)

    var version = 0

    fun save(): Memento? {
        val baseDirPath = Paths.get(Secrets.getSecrets().getHomeDir())

        val tmpFile = kotlin.io.path.createTempFile(directory = baseDirPath).toFile()
        logger.info("create tmp file at: {}", tmpFile.path)
        return null
        /*val chunkPath = Paths.get(Secrets.getSecrets().getHomeDir(), chunkHandle)
        val chunkFile = chunkPath.toFile()
        val tmpFile = kotlin.io.path.createTempFile(directory = baseDirPath).toFile()
        val tmpOutputStream = tmpFile.outputStream()
        try {
            val b = chunkFile.inputStream().readAllBytes();
            tmpOutputStream.write(b);
        } catch (exception: IOException) {
            logger.error(exception.toString())
            return null
        }
        return Memento(tmpFile)*/
    }

    fun restore(memento: Memento) {
    }

    fun writeChunk(mutation: FileProtocol.Mutation, chunkData: ChunkData) {
        val chunkPath = Paths.get(Secrets.getSecrets().getHomeDir(), chunkHandle)
        val chunkFile = chunkPath.toFile()
        chunkFile.outputStream().write(chunkData.data, mutation.offset, chunkData.data.size)
    }
}