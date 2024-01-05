package lgfs.gfs

class Lease(
    val chunkMetadata: ChunkMetadata, val ts: Long, val duration: Long, val primary: String, val replicas:
    List<String>
) {

    fun isValid(): Boolean {
        return true
    }

}