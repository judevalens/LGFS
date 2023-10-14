package lgfs.gfs

data class ChunkMetadata(val id: Long, val primaryServer: String) {
    val serverSet = HashSet<String>()
}
