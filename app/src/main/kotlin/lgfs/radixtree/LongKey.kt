package lgfs.radixtree

import utils.radixtree.Key
import java.nio.ByteBuffer

class LongKey(override var arr: ByteArray, override var key: Long, override var size: Int) : Key<Long>() {
     override fun sliceKey(range: IntRange): Key<Long> {
         TODO("Not yet implemented")
     }

     override fun getEmptyKey(): Key<Long> {
         TODO("Not yet implemented")
     }

     override fun concat(b: Key<Long>): Key<Long> {
         TODO("Not yet implemented")
     }
 }