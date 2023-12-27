package lgfs.gfs

data class ChunkMetadata(val handle: Long, val index : Long, val replicas: MutableList<String>)
