package lgfs.gfs

import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.network.JsonSerializable

class ChunkServerState @JsonCreator constructor(val chunkServerHostName: String) : JsonSerializable {
    var lastChunkCreateAt = -1
        private set
    fun diskUsage(): Pair<Long, Long> {
        return Pair(0, 10000);
    }

    private fun getDiskRation(): Int {
        return 10;
    }

    private val DISK_USAGE_WEIGHT = 0.75
    private val LCCA_WEIGHT = 0.5

    fun getRanking(): Double {
        return getDiskRation() * DISK_USAGE_WEIGHT + lastChunkCreateAt * LCCA_WEIGHT
    }

    fun updateLastAccessed() {

    }

}