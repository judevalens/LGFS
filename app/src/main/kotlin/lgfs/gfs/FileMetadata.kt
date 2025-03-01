package lgfs.gfs

data class FileMetadata(val path: String = "", val isDir: Boolean = false, val size: Long = 0, val offset: Long = 0)
