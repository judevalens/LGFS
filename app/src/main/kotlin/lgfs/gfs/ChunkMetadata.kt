package lgfs.gfs

data class ChunkMetadata(val handle: Long, val replicas: MutableList<String>)
