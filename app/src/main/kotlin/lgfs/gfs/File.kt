package lgfs.gfs;

import kotlin.math.ceil

class File (var metadata: FileMetadata) {
    private  var chunks: Array<ChunkMetadata>
    private lateinit var chunkHandles: HashMap<Long, Int>

    init {
        val numChunk = ceil((1 / FileSystem.CHUNK_SIZE).toDouble()).toInt()
        chunks = Array(numChunk) { chunkIndex ->
            val chunkHandle = FileSystem.lastChunkId.getAndIncrement()
            chunkHandles[chunkHandle] = chunkIndex
            ChunkMetadata(chunkHandle, "")
        }
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