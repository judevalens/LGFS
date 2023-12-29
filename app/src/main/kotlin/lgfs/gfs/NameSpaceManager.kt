package lgfs.gfs;


import org.apache.logging.log4j.LogManager
import utils.radixtree.Key
import lgfs.radixtree.RadixTree;
import java.nio.file.InvalidPathException
import java.nio.file.Paths

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue


class NameSpaceManager {
    private val rootMetaData: FileMetadata = FileMetadata("/", true, 0)
    private val root: RadixTree<String, FileMetadata> = RadixTree(Key.getStringKey(""), rootMetaData, Byte.SIZE_BITS)
    private val logger = LogManager.getLogger(javaClass.name)
    private val queue = LinkedBlockingQueue<Runnable>()
    private val executorService = Executors.newCachedThreadPool()

    fun makeDir(pathStr: String): Future<Boolean> {
        return executorService.submit<Boolean> {
            try {
                val path = Paths.get(pathStr)
                root.add(Key.getStringKey(pathStr), FileMetadata())
            } catch (e: InvalidPathException) {
                logger.error("Invalid path", e)
                false
            }
        }
    }
}

fun addFile() {

}
