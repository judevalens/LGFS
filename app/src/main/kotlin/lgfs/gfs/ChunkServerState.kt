package lgfs.gfs

import com.fasterxml.jackson.annotation.JsonCreator
import lgfs.network.JsonSerializable
import lgfs.network.ServerAddress

open class ChunkServerState @JsonCreator constructor(open val serverAddress: ServerAddress) : JsonSerializable {
    private var lastChunkCreateAt = -1

    companion object {
        private const val DISK_USAGE_WEIGHT = 0.75
        private const val LCCA_WEIGHT = 0.5
    }

    fun diskUsage(): Pair<Long, Long> {
        return Pair(0, 10000);
    }

    private fun getDiskRation(): Int {
        return 10;
    }

    open fun getRanking(): Double {
        return getDiskRation() * DISK_USAGE_WEIGHT + lastChunkCreateAt * LCCA_WEIGHT
    }

    fun updateLastAccessed() {
    }
}