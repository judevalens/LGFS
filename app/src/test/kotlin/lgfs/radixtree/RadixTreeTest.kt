package lgfs.radixtree

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.radixtree.Key
import utils.radixtree.RadixTree;

class TestRadixTree {
    private val nodes = arrayOf("romane", "romanus", "romulus", "rubens", "rubber", "rubicon", "rubicundus")
    private val paths = arrayOf("abc", "def", "abc/def", "def/abc", "xyz/abc", "xyz", "xyz/abc/def")

    @Test
    fun testFinding() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in nodes.indices) {
            tree.add(StringKey(nodes[i], Byte.SIZE_BITS), i)
        }
        for (i in nodes.indices) {
            assertEquals(i, tree.find(StringKey(nodes[i], Byte.SIZE_BITS)))
        }
    }

    @Test
    fun testFindingPaths() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in paths.indices) {
            tree.add(StringKey(paths[i], Byte.SIZE_BITS), i)
        }
        for (i in paths.indices) {
            assertEquals(i, tree.find(StringKey(paths[i], Byte.SIZE_BITS)))
        }
    }

    @Test
    fun testFindingNonExistent() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        assertNull(tree.find(StringKey("aebc", Byte.SIZE_BITS)))
        assertNull(tree.find(StringKey("aebc", Byte.SIZE_BITS)))
        assertNull(tree.find(StringKey("b", Byte.SIZE_BITS)))
    }

    @Test
    fun testDeleteNode() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in nodes.indices) {
            tree.add(StringKey(nodes[i], Byte.SIZE_BITS), i)
        }
        assertTrue(tree.delete(Key.getStringKey(nodes[0])));
        assertNull(tree.find(Key.getStringKey(nodes[0])));
        tree.add(Key.getStringKey(nodes[0]), 0);
        for (i in nodes.indices) {
            tree.add(StringKey(nodes[i], Byte.SIZE_BITS), i)
        }
    }

    @Test
    fun testDeleteInternalNode() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in paths.indices) {
            tree.add(StringKey(paths[i], Byte.SIZE_BITS), i)
        }
        assertNotNull(tree.find(Key.getStringKey("xyz")));
        assertNotNull(tree.delete(Key.getStringKey("xyz")));
        assertNull(tree.find(Key.getStringKey("xyz")));
        assertNotNull(tree.find(Key.getStringKey("xyz/abc")));
    }

}
