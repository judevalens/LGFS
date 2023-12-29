package lgfs.radixtree

import utils.radixtree.Key
import utils.radixtree.Node
import java.util.*

class RadixTree<K, V>(private var key: Key<K>, private var value: V, private val radix: Int) {
    private var root: Node<K, V> = Node(key, value)
    fun add(key: Key<K>, value: V, update: Boolean): Boolean {
        println("adding: ${key.toString()}")
        if (!update) {
            if (find(key) != null) {
                println("found value ${find(key)}")
                return false
            }
        }
        val nodesStack: Queue<Node<K, V>> = ArrayDeque(Collections.singleton(root))
        val lockStack = Stack<Node<K, V>>();
        var i = 0
        try {
            while (!nodesStack.isEmpty()) {
                val currentNode = nodesStack.remove()
                currentNode.lock.writeLock()
                currentNode.lock.lockDelete()
                println("got lock")
                lockStack.add(currentNode)
                try {
                    val l: Int = currentNode.key.length()
                    val r = key.length()
                    val j: Int = keyCompare(currentNode.key.getByteArray(), key.getByteArray().sliceArray(i..<r))
                    i += j
                    if (i > 0) {
                        val currentNodeSuffixRange = j..<l
                        val suffixKeyRange = i..<r
                        val currentNodePrefixRange = 0..<j

                        println(
                            "prefix: ${currentNode.key.sliceKey(currentNodePrefixRange)}, currentNode suffix: ${
                                currentNode.key.sliceKey(
                                    currentNodeSuffixRange
                                )
                            }, suffix: ${key.sliceKey(suffixKeyRange)}"
                        )
                        if (currentNodeSuffixRange.isEmpty() && suffixKeyRange.isEmpty()) {
                            currentNode.value = value
                        } else if (!currentNodeSuffixRange.isEmpty() && !suffixKeyRange.isEmpty()) {
                            val extendedNode: Node<K, V> =
                                Node(currentNode.key.sliceKey(currentNodeSuffixRange), currentNode.value)
                            extendedNode.nodes = currentNode.nodes
                            currentNode.key = currentNode.key.sliceKey(currentNodePrefixRange)
                            currentNode.value = null
                            currentNode.nodes = LinkedList<Node<K, V>>()
                            currentNode.nodes.add(extendedNode)
                            currentNode.nodes.add(Node(key.sliceKey(suffixKeyRange), value))
                        } else if (!currentNodeSuffixRange.isEmpty()) {
                            val extendedNode: Node<K, V> =
                                Node(currentNode.key.sliceKey(currentNodeSuffixRange), currentNode.value)
                            currentNode.key = currentNode.key.sliceKey(currentNodePrefixRange)
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
                        return true
                    }
                    nodesStack.addAll(currentNode.nodes)
                } finally {
                    currentNode.lock.unlockWrite()
                }
            }
            root.nodes.add(Node(key, value))
        } finally {
            for (lock in lockStack) {
                println("releasing locks")
                lock.lock.unlockDelete()
            }
        }

        return true
    }

    fun add(key: Key<K>, value: V): Boolean {
        return add(key, value, false)
    }

    fun find(key: Key<K>): V? {
        val nodesStack: Queue<Node<K, V>> = ArrayDeque(root.nodes)

        var i = 0
        while (!nodesStack.isEmpty() && i < key.length()) {
            val currentNode = nodesStack.remove();
            //System.out.printf("search prefix: %s, current node key: %s\n", key.substring(i), currentNode.key);

            val j = keyCompare(currentNode.key.getByteArray(), key.getByteArray().sliceArray(i..<key.length()))
            i += j
            if (j > 0) {
                // if we reach the end of the key and the current's node key is not longer the wanted key, we must have found the correct node
                if (i == key.length() && currentNode.key.length() <= i) {
                    return currentNode.value;
                }
                nodesStack.clear();
                nodesStack.addAll(currentNode.nodes);
            }
        }

        return null;
    }

    fun delete(key: Key<K>): Boolean {

        val nodesStack: Queue<Node<K, V>> = ArrayDeque(root.nodes)
        val parentsStack: Stack<Node<K, V>> = Stack()
        parentsStack.add(root)
        var i = 0
        while (!nodesStack.isEmpty() && i < key.length()) {
            val currentNode: Node<K, V> = nodesStack.remove()
            val j = keyCompare(currentNode.key.getByteArray(), key.getByteArray().sliceArray(i..<key.length()))
            i += j
            if (j > 0) {
                if (i == key.length()) {
                    currentNode.value = null
                    val parent: Node<K, V> = parentsStack.pop()
                    println("deleting node: " + key + " | parent key: " + parent.key)
                    if (parent.nodes.size == 1 && parent.value == null) {
                        val onlyChild = parent.nodes.remove()
                        System.out.printf(
                            "merging parent and child: parent key: %s, child key: %s\n",
                            parent.key,
                            onlyChild.key
                        )
                        parent.key = parent.key.concat(onlyChild.key)
                        parent.value = onlyChild.value
                    }
                    return true
                }
                parentsStack.add(currentNode)
                nodesStack.addAll(currentNode.nodes)
            }
        }
        return false
    }

    fun printTree() {
        printTree(root, root.key.getEmptyKey())
    }

    private fun printTree(node: Node<K, V>, prefix: Key<K>) {
        for (n in node.nodes) {
            printTree(n, prefix.concat(node.key))
        }
        if (node.value == null && !node.nodes.isEmpty()) {
            return
        }
        println("prefix: $prefix")
        println("suffix: ${node.key}")
        println(prefix.toString() + node.key.toString() + " | value: " + node.value)
        println("*************************")
    }

    private fun keyCompare(a: ByteArray, b: ByteArray): Int {
        var byteIndex = 0
        if (a.size % (radix / 8) != 0 || b.size % (radix / 8) != 0) return -100000000
        while (byteIndex < a.size && byteIndex < b.size) {
            for (i in 0..<radix / 8) {
                if (a[byteIndex + i] != b[byteIndex + i]) return byteIndex
            }
            byteIndex += radix / 8;
        }
        return byteIndex;
    }

}