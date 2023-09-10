package utils.radixtree

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
    abstract fun length(): Int
    abstract fun getEmptyKey() : Key<E>
    companion object Factory {
        fun getStringKey(key: String): StringKey {
            return StringKey(key, Byte.SIZE_BITS);
        }
    }

}