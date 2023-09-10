import utils.radixtree.RadixTree;
import utils.radixtree.StringKey

fun main() {
    val nodes = arrayOf("romane", "romanus", "romulus", "rubens", "rubber", "rubicon", "rubicundus")
    val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
    for (i in nodes.indices) {
        tree.add(StringKey(nodes[i], Byte.SIZE_BITS), i)
    }

    tree.printTree()
}