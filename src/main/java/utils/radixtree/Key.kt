package utils.radixtree

abstract class Key<E> constructor(size: Int) {
    lateinit var arr: ByteArray
    abstract fun getByteArray(): Array<Byte>
    abstract fun sliceKey(range: IntRange): Key<E>
     fun isPrefix(B: Key<E>): Boolean {
         return arr[0] == B.getByteArray()[0]
     }
    abstract fun length(): Int
}