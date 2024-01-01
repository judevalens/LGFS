package lgfs.gfs

import utils.radixtree.Key
import lgfs.radixtree.RadixTree
import java.util.concurrent.atomic.AtomicLong

class FileSystem {
    companion object {
        val lastChunkId = AtomicLong()
        const val CHUNK_SIZE = 64 * 1000000
    }

    private val radixTree = RadixTree(Key.getStringKey(""), FileMetadata("/", true, 0), Byte.SIZE_BITS);

    /// contains mapping from chunk handle to file path
    private val chunksLookupTable = HashMap<Long, String>()

    fun addFile(metadata: FileMetadata): Boolean {
        return radixTree.add(Key.getStringKey(metadata.path), metadata)
    }

    fun deleteFile(fileName: String): Boolean {
        return radixTree.delete(Key.getStringKey(fileName))
    }

    fun printFs() {
        radixTree.printTree()
    }

    fun attachServerToChunk(serverHostName: String, chunkHandle: Long): Boolean {
        val filePath = chunksLookupTable[chunkHandle]
        filePath?.let {
            val file = radixTree.find(Key.getStringKey(filePath))
            file?.let {
                TODO()//return it.attachServerToChunk(chunkHandle, serverHostName)
            }
        }
        return false
    }
}