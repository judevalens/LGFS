package lgfs.gfs

class Lease(val chunkId: Long, val ts: Long) {

    fun isValid() : Boolean {
        return true
    }

}