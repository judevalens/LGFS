package lgfs.gfs

import lgfs.network.Secrets
import java.nio.file.Paths
import java.util.*

class ChunkFile(private val chunkPath: String) {

    class Memento(val filePath: String) {
    }

    var version = 0
    var tmpPath = Paths.get(Secrets.getSecrets().getHomeDir(), "/tmp/", chunkPath)
    fun save(): Memento {
        val chunkPath = Paths.get(Secrets.getSecrets().getHomeDir(), chunkPath)
        val chunkFile = chunkPath.toFile()

        val tmpFile = Paths.get(Secrets.getSecrets().getHomeDir(), UUID.randomUUID().toString()).toFile()
        val tmpOutputStream = tmpFile.outputStream()

        val b = tmpFile.inputStream().read();
        while (b > -1) {
            tmpOutputStream.write(b);
        }

        return Memento(tmpFile.path)
    }

    fun restore(memento: Memento) {

    }

    fun writeChunk(mutation: FileProtocol.Mutation, chunkData: ChunkData) {
        val chunkPath = Paths.get(Secrets.getSecrets().getHomeDir(), chunkPath)
        val chunkFile = chunkPath.toFile()
        TODO()
        //chunkFile.outputStream().write(chunkData.data, mutation.offset, chunkData.data.size)
    }

}