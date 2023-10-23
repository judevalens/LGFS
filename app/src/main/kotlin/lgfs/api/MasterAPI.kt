package lgfs.api

import java.util.concurrent.Future

interface MasterAPI {
    class FileMetadata(val path: String, val size: Int)


    fun createFile(fileMetadata: lgfs.gfs.FileMetadata): Future<Boolean>
}