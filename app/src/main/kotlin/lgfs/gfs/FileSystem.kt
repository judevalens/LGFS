package lgfs.gfs

import utils.radixtree.Key
import utils.radixtree.RadixTree
import java.util.concurrent.atomic.AtomicLong

class FileSystem {
    companion object {
        val lastChunkId = AtomicLong()
        const val CHUNK_SIZE = 64 * 1000000
    }

    private val radixTree = RadixTree(Key.getStringKey("/"), FileMetadata(), Byte.SIZE_BITS);
    private val chunksLookupTable = HashMap<Long, String>()

    fun addFile(metadata: FileMetadata): Boolean {
        val added = radixTree.add(Key.getStringKey(metadata.path), metadata)
        return added
    }

    fun printFs() {
        radixTree.printTree()
    }

    fun attachServerToChunk(serverHostName: String, chunkHandle: Long): Boolean {
        val filePath = chunksLookupTable[chunkHandle]
        filePath?.let {
            val file = radixTree.find(Key.getStringKey(filePath))
            file?.let {
                return it.attachServerToChunk(chunkHandle, serverHostName)
            }
        }
        return false
    }
}