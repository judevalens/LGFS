package lgfs.gfs

import kotlin.math.ceil

class FileMetadata(val path: String, val isDir: Boolean, val size: Long) {

    private var chunks: Array<ChunkMetadata>
    private lateinit var chunkHandles: HashMap<Long, Int>

    init {
        val numChunk = ceil(size.toDouble() / FileSystem.CHUNK_SIZE).toInt()
        chunks = Array(numChunk) { chunkIndex ->
            val chunkHandle = FileSystem.lastChunkId.getAndIncrement()
            chunkHandles[chunkHandle] = chunkIndex
            ChunkMetadata(chunkHandle, "")
        }
    }

    constructor() : this("", true, -1) {
    }

    companion object {
        fun newFile(path: String, size: Long): FileMetadata {
            return FileMetadata(path, false, size)
        }
    }

    fun attachServerToChunk(chunkHandle: Long, serverHostname: String): Boolean {
        val chunkIndex = chunkHandles[chunkHandle]
        chunkIndex?.let {
            chunks[chunkIndex].serverSet.add(serverHostname)
            return true
        }
        return false
    }
}
