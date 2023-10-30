package lgfs.gfs

class FileMetadata(val path: String, val isDir: Boolean, val size: Long) {
    constructor() : this("", true, 0)
}
