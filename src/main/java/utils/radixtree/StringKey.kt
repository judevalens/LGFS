package utils.radixtree

import java.nio.charset.Charset

class StringKey(override var key: String, override var size: Int, override var arr: ByteArray) : Key<String>() {
    constructor(key: String, size: Int) : this(key, size, key.toByteArray()) {
    }

    override fun sliceKey(range: IntRange): Key<String> {
        return StringKey(arr.sliceArray(range).toString(Charset.defaultCharset()), Byte.SIZE_BITS);
    }

    override fun concat(b: Key<String>): Key<String> {
        return StringKey(key + b.key, Byte.SIZE_BITS)
    }

    override fun length(): Int {
        return arr.size
    }

    override fun getEmptyKey(): Key<String> {
        return StringKey("",Byte.SIZE_BITS)
    }

    override fun toString(): String {
        return key;
    }

}