package gfs

data class ChunkMetadata(val id: Long, val servers: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkMetadata

        if (id != other.id) return false
        if (!servers.contentEquals(other.servers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + servers.contentHashCode()
        return result
    }
}
