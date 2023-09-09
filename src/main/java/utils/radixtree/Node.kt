package utils.radixtree;

import java.util.LinkedList;

class Node<K,V> constructor(var key: Key<K>, var value: V?) {

    var nodes: LinkedList<Node<K,V>> = LinkedList()

    fun isRLocked(): Boolean {
        return false;
    }

    fun isWLocked(): Boolean {
        return false;
    }
}
