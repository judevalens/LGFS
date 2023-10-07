import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert;
import org.junit.Test;
import utils.radixtree.Key
import utils.radixtree.RadixTree;
import utils.radixtree.StringKey;
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.locks.Condition
import kotlin.test.assertFalse

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
            Assert.assertEquals(i, tree.find(StringKey(nodes[i], Byte.SIZE_BITS)))
        }
    }

    @Test
    fun testFindingPaths() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in paths.indices) {
            tree.add(StringKey(paths[i], Byte.SIZE_BITS), i)
        }
        for (i in paths.indices) {
            Assert.assertEquals(i, tree.find(StringKey(paths[i], Byte.SIZE_BITS)))
        }
    }

    @Test
    fun testFindingNonExistent() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        Assert.assertNull(tree.find(StringKey("aebc", Byte.SIZE_BITS)));
        Assert.assertNull(tree.find(StringKey("aebc", Byte.SIZE_BITS)));
        Assert.assertNull(tree.find(StringKey("b", Byte.SIZE_BITS)));
    }

    @Test
    fun testDeleteNode() {
        val tree = RadixTree(StringKey("/", Byte.SIZE_BITS), 0, Byte.SIZE_BITS)
        for (i in nodes.indices) {
            tree.add(StringKey(nodes[i], Byte.SIZE_BITS), i)
        }
        Assert.assertTrue(tree.delete(Key.getStringKey(nodes[0])));
        Assert.assertNull(tree.find(Key.getStringKey(nodes[0])));
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
        Assert.assertNotNull(tree.find(Key.getStringKey("xyz")));
        Assert.assertNotNull(tree.delete(Key.getStringKey("xyz")));
        Assert.assertNull(tree.find(Key.getStringKey("xyz")));
        Assert.assertNotNull(tree.find(Key.getStringKey("xyz/abc")));
    }
}
