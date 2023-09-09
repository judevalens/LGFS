package utils.radixtree

import java.util.*

class RadixTree<K, V> constructor(private var key: Key<K>, private var value: V, private val radix: Int) {
    private var root: Node<K, V> = Node(key, value)

    fun add(key: Key<K>, value: V): Boolean {
        val nodesStack: Queue<Node<K, V>> = ArrayDeque()
        var i = 0
        while (!nodesStack.isEmpty()) {
            val currentNode = nodesStack.remove()
            val l: Int = currentNode.key.length()
            val r = key.length()
            var j = 0

            i += keyCompare(currentNode.key.getByteArray(), key.getByteArray().sliceArray(i..r))

            if (i > 0) {
                val currentNodeSuffixRange = j..l
                val suffixKeyRange = i..r
                if (currentNode.isWLocked()) {
                    return false
                }
                if (currentNodeSuffixRange.isEmpty() && suffixKeyRange.isEmpty()) {
                    currentNode.value = value
                    println("updated value!")
                } else if (!currentNodeSuffixRange.isEmpty() && !suffixKeyRange.isEmpty()) {
                    currentNode.key = currentNode.key.sliceKey(0..j)
                    currentNode.key.sliceKey(currentNodeSuffixRange)
                    val extendedNode: Node<K, V> =
                        Node(currentNode.key.sliceKey(currentNodeSuffixRange), currentNode.value)
                    extendedNode.nodes = currentNode.nodes
                    currentNode.value = null
                    currentNode.nodes = LinkedList<Node<K, V>>()
                    currentNode.nodes.add(extendedNode)
                    currentNode.nodes.add(Node(key.sliceKey(suffixKeyRange), value))
                } else if (!currentNodeSuffixRange.isEmpty()) {
                    val extendedNode: Node<K, V> =
                        Node(currentNode.key.sliceKey(currentNodeSuffixRange), currentNode.value)
                    currentNode.key = currentNode.key.sliceKey(0..j)
                    currentNode.value = value
                    extendedNode.nodes = currentNode.nodes
                    currentNode.nodes = LinkedList<Node<K, V>>()
                    currentNode.nodes.add(extendedNode)
                } else if (!suffixKeyRange.isEmpty()) {
                    var foundEdge = false
                    val suffixKey = key.sliceKey(suffixKeyRange)
                    for (node in currentNode.nodes) {
                        if (node.key.isPrefix(suffixKey)) {
                            nodesStack.clear()
                            nodesStack.add(node)
                            foundEdge = true
                            break
                        }

                    }
                    if (foundEdge) continue
                    currentNode.nodes.add(Node(suffixKey, value))
                }
                println(
                    "prefix: ${currentNode.key.sliceKey(0..j)}, currentNode suffix: ${
                        currentNode.key.sliceKey(
                            currentNodeSuffixRange
                        )
                    }, suffix: ${key.sliceKey(suffixKeyRange)}"
                )
                return true
            }
            nodesStack.addAll(currentNode.nodes)
        }
        return false
    }

    fun find(key: Key<K>): V? {
        val nodesStack: Queue<Node<K, V>> = ArrayDeque(root.nodes)

        var i = 0
        while (!nodesStack.isEmpty() && i < key.length()) {
            var currentNode = nodesStack.remove();
            //System.out.printf("search prefix: %s, current node key: %s\n", key.substring(i), currentNode.key);

            val j = keyCompare(currentNode.key.getByteArray(), key.getByteArray())

            if (j + i > i) {
                i += j
                if (i == key.length()) {
                    return currentNode.value;
                }
                nodesStack.clear();
                nodesStack.addAll(currentNode.nodes);
            }
        }

        return null;
    }

    private fun keyCompare(A: Array<Byte>, B: Array<Byte>): Int {
        var byteIndex = 0
        if (A.size % radix != 0 || B.size % radix != 0) return -1
        while (byteIndex < A.size && byteIndex < B.size) {
            for (i in 0..radix) {
                if (A[i] != B[i]) break
            }
            byteIndex += radix;
        }
        return byteIndex;
    }
}