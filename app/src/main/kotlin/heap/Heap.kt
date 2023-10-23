package heap

import java.util.Objects

class Heap<T : Comparable<T>> {
    private val heap = Array(1000) {  Any() }

    fun add(e: Int) {

    }

    fun add(e: T, parentIdx: Int) {
        val parent = heap[parentIdx] as T
        if (parent < e) {

        }
    }
}