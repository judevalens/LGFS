package gfs

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.ceil

class FileMetadata(key: String, isDir: Boolean, size: Int) : ReadWriteLock {
    var key: String? = null
    var size: Long = 0
    var isDir = false
    val lock = ReentrantReadWriteLock();
    private var chunks: ArrayList<Chunk>? = null

    constructor() : this("", true, -1) {
    }

    init {
        val numChunks = ceil(size / FileSystem.CHUNK_SIZE.toDouble()).toInt()
        chunks = ArrayList(numChunks)
    }

    override fun readLock(): Lock {
        return lock.readLock();
    }

    override fun writeLock(): Lock {
        return lock.writeLock()
    }
}
