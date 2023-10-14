package utils.radixtree

import utils.radixtree.concurrent.Lock
import java.util.LinkedList


class Node<K, V>(var key: Key<K>, var value: V?) {
    var nodes: LinkedList<Node<K, V>> = LinkedList()
    var isInternal = false
    var  lock = Lock()
        private set
}
