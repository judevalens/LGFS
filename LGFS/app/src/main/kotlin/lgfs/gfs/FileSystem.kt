package lgfs.gfs

import utils.radixtree.Key
import utils.radixtree.RadixTree
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil

class FileSystem {
    companion object {
        const val CHUNK_SIZE = 64 * 1000000
    }

    private var lastChunkId = AtomicLong()
    private val radixTree = RadixTree(Key.getStringKey("/"), FileMetadata(), Byte.SIZE_BITS);
    private val chunksLookupTable = RadixTree(Key.ge)
    fun addFile(metadata: FileMetadata): Boolean {
        val added = radixTree.add(Key.getStringKey(metadata.path), metadata)
        val numChunk = ceil(metadata.size.toDouble() / CHUNK_SIZE).toInt()
        val chunkMap = HashMap<Int, ChunkMetadata>()
        for (i in 0..numChunk) {
            val chunkId = lastChunkId.getAndIncrement()
            chunkMap[i] = ChunkMetadata(chunkId, "")
        }
        val file = File(metadata, chunkMap)
        return added
    }

    fun printFs() {
        radixTree.printTree()
    }

    fun attachServerToChunk(serverHostName: String, chunkId: Long) {

    }
}