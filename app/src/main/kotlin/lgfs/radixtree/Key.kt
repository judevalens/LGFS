package utils.radixtree

import lgfs.radixtree.LongKey
import lgfs.radixtree.StringKey
import java.nio.ByteBuffer

abstract class Key<E> {
    abstract var arr: ByteArray
    abstract var key: E
    abstract var size: Int
    fun getByteArray(): ByteArray {
        return arr
    }

    abstract fun sliceKey(range: IntRange): Key<E>
    fun isPrefix(B: Key<E>): Boolean {
        return arr[0] == B.getByteArray()[0]
    }
    abstract fun concat(b: Key<E>): Key<E>
     fun length(): Int {
        return arr.size
    }
    abstract fun getEmptyKey() : Key<E>
    companion object Factory {
        fun getStringKey(key: String): StringKey {
            return StringKey(key, Byte.SIZE_BITS);
        }

        fun getLongKey(key: Long): LongKey {
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            buffer.putLong(key)
            buffer.array()
            return LongKey(buffer.array(),key,Long.SIZE_BYTES)
        }
    }

}