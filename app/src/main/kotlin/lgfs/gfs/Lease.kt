package lgfs.gfs

class Lease(val chunkId: Long, val ts: Long, val duration: Long, val primary: String, val replicas: List<String>) {

    fun isValid(): Boolean {
        return true
    }

}