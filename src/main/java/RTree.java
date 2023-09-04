import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class RTree {
    private Node root;

    public RTree() {
    }

    public void add(String key, Object value) {
        Queue<Node> nodesStack = new ArrayDeque<>();
        if (root == null) {
            root = new Node("", null);
        }
        nodesStack.add(root);
        int i = 0;
        Node currentNode = null;
        while (!nodesStack.isEmpty()) {
             currentNode = nodesStack.remove();
            var l = currentNode.key.length();
            var r = key.length();

            while (i < l && i < r && currentNode.key.substring(i, i + 1).equals(key.substring(i, i+1))) {
                i++;
            }

            if (i > 0) {
                var currentNodeSuffixKey = currentNode.key.substring(i,l);
                var suffixKey = key.substring(i,r);
                currentNode.key = currentNode.key.substring(0,i);
                var extendedNode = new Node(currentNodeSuffixKey,currentNode.value);
                extendedNode.nodes = currentNode.nodes;
                currentNode.nodes = new LinkedList<>();

                System.out.println(extendedNode.nodes==currentNode.nodes);
                currentNode.nodes.add(extendedNode);
                currentNode.nodes.add(new Node(suffixKey,value));
                System.out.printf("prefix: %s, currentNode suffix: %s, new Node: suffix: %s\n",currentNode.key.substring(0,i),currentNodeSuffixKey,suffixKey);
                return;
            }else {
                nodesStack.addAll(currentNode.nodes);
            }

        }
        root.nodes.add(new Node(key,value));
    }

    public void printTree() {
       printTree(root,"");
    }

    public void printTree(Node node, String prefix) {

        for(Node n: node.nodes) {
            printTree(n,prefix+node.key);
        }
        System.out.println(prefix);
        System.out.println(node.key);
        System.out.println(prefix+node.key);
        System.out.println("*************************");
    }
}
