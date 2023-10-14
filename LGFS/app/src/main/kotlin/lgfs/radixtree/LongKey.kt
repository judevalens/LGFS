package lgfs.radixtree

import utils.radixtree.Key

abstract class LongKey(override var arr: ByteArray, override var key: Long, override var size: Int) : Key<Long>() {
}