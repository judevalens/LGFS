package lgfs.gfs

class FileMetadata(val path: String, val isDir: Boolean, private val size: Long) {
    constructor() : this("", true, 0)
}
