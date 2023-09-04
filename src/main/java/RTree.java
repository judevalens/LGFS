import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class RTree {
    private Node root;

    public RTree() {
    }

    public void add(String key, Object value) {
        Queue<Node> nodesStack = new ArrayDeque<>();
        if (root == null) {
            root = new Node("/", null);
        }
        nodesStack.add(root);
        int i = 0;
        Node currentNode = null;
        while (!nodesStack.isEmpty()) {
            currentNode = nodesStack.remove();
            var l = currentNode.key.length();
            var r = key.length();
            int j = 0;
            while (i < l && i < r && currentNode.key.substring(j, j + 1).equals(key.substring(i, i + 1))) {
                i++;
                j++;
            }

            if (i > 0) {
                var currentNodeSuffixKey = currentNode.key.substring(i, l);
                var suffixKey = key.substring(i, r);

                if (currentNodeSuffixKey.isEmpty() && suffixKey.isEmpty()) {
                    currentNode.value = value;
                    System.out.println("updated value!");
                    return;
                } else if (!currentNodeSuffixKey.isEmpty() && !suffixKey.isEmpty()) {
                    currentNode.key = currentNode.key.substring(0, i);
                    var extendedNode = new Node(currentNodeSuffixKey, currentNode.value);
                    currentNode.value = null;
                    extendedNode.nodes = currentNode.nodes;
                    currentNode.nodes = new LinkedList<>();
                    currentNode.nodes.add(extendedNode);
                    currentNode.nodes.add(new Node(suffixKey, value));
                } else if (currentNodeSuffixKey.isEmpty()) {
                    currentNode.nodes.add(new Node(suffixKey, value));
                } else if (suffixKey.isEmpty()) {
                    // new key is the prefix of an existing key
                    var extendedNode = new Node(currentNodeSuffixKey, currentNode.value);
                    extendedNode.nodes = currentNode.nodes;
                    currentNode.key = key;
                    currentNode.value = value;
                    currentNode.nodes = new LinkedList<>(Collections.singletonList(extendedNode));
                    System.out.println("extending existing! | key: " + key);
                }
                System.out.printf("prefix: %s, currentNode suffix: %s, new Node: suffix: %s\n", currentNode.key.substring(0, i), currentNodeSuffixKey, suffixKey);
                return;
            } else {
                nodesStack.addAll(currentNode.nodes);
            }

        }
        root.nodes.add(new Node(key, value));
    }

    public Object find(String key) {
        Queue<Node> nodesStack = new ArrayDeque<>();
        if (root == null) {
            return null;
        }
        nodesStack.add(root);
        int i = 0;
        while (!nodesStack.isEmpty() && i < key.length()) {
            var currentNode = nodesStack.remove();
            int j = 0;
            while (i < key.length() && j < currentNode.key.length() && key.substring(i, i + 1).equals(currentNode.key.substring(j, j + 1))) {
                i++;
                j++;
            }

            if (i > 0) {

                if (i == key.length()) {
                    return currentNode.value;
                }

                nodesStack.addAll(currentNode.nodes);
            }
        }

        return null;
    }

    public void printTree() {
        printTree(root, "");
    }

    public void printTree(Node node, String prefix) {

        for (Node n : node.nodes) {
            printTree(n, prefix + node.key);
        }
        System.out.println(prefix);
        System.out.println(node.key);
        System.out.println(prefix + node.key + " | value: " + node.value);
        System.out.println("*************************");
    }
}
