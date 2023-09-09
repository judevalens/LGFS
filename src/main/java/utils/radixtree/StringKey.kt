package utils.radixtree

class StringKey(key: String, size: Int) : Key<String>(size) {
    override fun getByteArray(): Array<Byte> {
        TODO("Not yet implemented")
    }

    override fun sliceKey(range: IntRange): Key<String> {
        TODO("Not yet implemented")
    }

    override fun length(): Int {
        TODO("Not yet implemented")
    }
}