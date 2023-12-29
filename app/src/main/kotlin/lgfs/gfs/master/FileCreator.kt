package lgfs.gfs.master

import lgfs.gfs.ChunkMetadata
import lgfs.gfs.FileMetadata
import lgfs.gfs.FileSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FileCreator(
    private val reqId: String,
    private val fileMetadata: FileMetadata,
    private val fs: FileSystem
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Ask the [StateManagerActor] where to put this file's chunk
     */

    private fun createFile(chunks: List<ChunkMetadata>?): Pair<Boolean, List<ChunkMetadata>?> {
        logger.info("{} - creating file : {} in FS", reqId, fileMetadata.path)
        if (chunks?.isEmpty() == true) {
            logger.info("{} - failed to allocate chunks for file : {}", reqId, fileMetadata.path)
            return Pair(false, null)
        }
        val fileCreated = fs.addFile(fileMetadata)
        return if (fileCreated) {
            logger.info(
                "{} - Created file at: {}", reqId,fileMetadata.path)
            Pair(true, chunks)
        } else {
            logger.info("{} - Failed to add file path: {} to directory tree", reqId, fileMetadata.path)
            Pair(true, null)
        }
    }
}