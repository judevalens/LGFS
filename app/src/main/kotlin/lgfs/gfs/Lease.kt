package lgfs.gfs

class Lease(val chunkHandle: Long, val ts: Long, val duration: Long, val primary: String, val replicas: List<String>) {

    fun isValid(): Boolean {
        return true
    }

}