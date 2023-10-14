package lgfs.gfs

class FileMetadata(val path: String, val isDir: Boolean, val size: Long)  {

    val chunkMap = HashMap<Int,Long>()

    constructor() : this("", true, -1) {
    }

    companion object {
        fun newFile(path: String, size: Long): FileMetadata {
            return FileMetadata(path,false,size)
        }
    }
}
