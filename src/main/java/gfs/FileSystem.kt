package gfs

import utils.radixtree.Key
import utils.radixtree.RadixTree
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class FileSystem {
    companion object {
        const val CHUNK_SIZE = 64 * 1000000
    }
    private var lastChunkId = AtomicLong()
    private val radixTree = RadixTree<String, FileMetadata>(Key.getStringKey("/"), FileMetadata(), Byte.SIZE_BITS);
    fun addFile(metadata: FileMetadata): Boolean {
         val added = radixTree.add(Key.getStringKey(metadata.path), metadata)
        val File = File(metadata)
        val numChunk = ceil(metadata.size.toDouble() / CHUNK_SIZE).toInt()

        for (i in 0..numChunk) {
            val chunkId = lastChunkId.getAndIncrement()
            val newChunk = ChunkMetadata(chunkId, arrayOf(""));
        }
        return added
    }
    fun printFs() {
        radixTree.printTree()
    }
}