package lgfs.api

interface Client {
    class FileMetadata(val path: String, val size: Int)
    fun createFile(fileMetadata: FileMetadata) : Boolean
}