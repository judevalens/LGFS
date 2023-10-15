package lgfs.gfs

data class ChunkMetadata(val handle: Long, val primaryServer: String) {
    val serverSet = HashSet<String>()
}
