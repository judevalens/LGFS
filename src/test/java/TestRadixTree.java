import org.junit.Assert;
import org.junit.Test;
import utils.radixtree.RadixTree;

public class TestRadixTree {
    String[] nodes = {"romane", "romanus", "romulus", "rubens", "rubber", "rubicon", "rubicundus"};
    String[] paths = {"abc", "def", "abc/def", "def/abc", "xyz/abc", "xyz", "xyz/abc/def"};

    @Test
    public void testFinding() {
        RadixTree<Integer> rTree = new RadixTree<>();
        for (int i = 0; i < nodes.length; i++) {
            rTree.add(nodes[i], i);
        }
        for (int i = 0; i < nodes.length; i++) {
            Assert.assertEquals(rTree.find(nodes[i]), i);
        }
    }

    @Test
    public void testFindingPaths() {
        RadixTree<Integer> rTree = new RadixTree<>();
        for (int i = 0; i < paths.length; i++) {
            rTree.add(paths[i], i);
        }
        rTree.printTree();
        for (int i = 0; i < paths.length; i++) {
            Assert.assertEquals(rTree.find(paths[i]), i);
        }
    }

    @Test
    public void testFindingNonExistent() {
        RadixTree<Integer> tree = new RadixTree<>();

        tree.find("aebc");
        tree.find("aebd");

        Assert.assertNull(tree.find("b"));

    }

    @Test
    public void testDeleteNode() {
        RadixTree<Integer> rTree = new RadixTree<>();
        for (int i = 0; i < nodes.length; i++) {
            rTree.add(nodes[i], i);
        }
        Assert.assertTrue(rTree.delete(nodes[0]));
        Assert.assertNull(rTree.find(nodes[0]));
        rTree.add(nodes[0], 0);
        for (int i = 0; i < nodes.length; i++) {
            Assert.assertEquals(rTree.find(nodes[i]), i);
        }
    }

    @Test
    public void testDeleteInternalNode() {
        RadixTree<Integer> rTree = new RadixTree<>();
        for (int i = 0; i < paths.length; i++) {
            rTree.add(paths[i], i);
        }
        var idx = (int) rTree.find("xyz");
        Assert.assertNotNull(rTree.find("xyz"));
        Assert.assertTrue(rTree.delete("xyz"));
        Assert.assertNull(rTree.find("xyz"));
        Assert.assertNotNull(rTree.find("xyz/abc"));
    }
}
