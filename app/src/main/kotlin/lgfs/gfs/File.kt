package lgfs.gfs;

class File(var metadata: FileMetadata) {
    private lateinit var chunks: HashMap<Long, ChunkMetadata>

    companion object {
        fun newFile(path: String, size: Long): FileMetadata {
            return FileMetadata(path, false, size)
        }
    }

    fun attachServerToChunk(chunkHandle: Long, serverHostname: String): Boolean {
        chunks[chunkHandle]!!.replicas.add(serverHostname)
        return false
    }
}