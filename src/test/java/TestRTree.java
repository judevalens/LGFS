import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class TestRTree {
    String[] nodes = {"romane","romanus","romulus","rubens","rubber","rubicon","rubicundus"};
    String[] paths = {"abc","def","abc/def","def/abc","xyz","xyz/abc","xyz/abc/def"};

    @Test
    public void testFinding() {
        RTree rTree = new RTree();
        for (int i = 0; i < nodes.length; i++) {
            rTree.add(nodes[i],i);
        }
        rTree.printTree();
        for (int i = 0; i < nodes.length; i++) {
            Assert.assertEquals(rTree.find(nodes[i]),i);
        }
    }

    @Test
    public void testFindingPaths(){
        RTree rTree = new RTree();
        for (int i = 0; i < paths.length; i++) {
            rTree.add(paths[i],i);
        }
        rTree.printTree();
        for (int i = 0; i < paths.length; i++) {
            Assert.assertEquals(rTree.find(paths[i]),i);
        }
    }
}
