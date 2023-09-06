import java.util.*;

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
            while (j < l && i < r && currentNode.key.substring(j, j + 1).equals(key.substring(i, i + 1))) {
                i++;
                j++;
            }

            if (i > 0) {
                var currentNodeSuffixKey = currentNode.key.substring(j, l);
                var suffixKey = key.substring(i, r);
                if (currentNodeSuffixKey.isEmpty() && suffixKey.isEmpty()) {
                    currentNode.value = value;
                    System.out.println("updated value!");
                    return;
                } else if (!currentNodeSuffixKey.isEmpty() && !suffixKey.isEmpty()) {
                    currentNode.key = currentNode.key.substring(0, j);
                    var extendedNode = new Node(currentNodeSuffixKey, currentNode.value);
                    extendedNode.nodes = currentNode.nodes;
                    currentNode.value = null;
                    currentNode.nodes = new LinkedList<>();
                    currentNode.nodes.add(extendedNode);
                    currentNode.nodes.add(new Node(suffixKey, value));
                } else if (!currentNodeSuffixKey.isEmpty()) {
                    var extendedNode = new Node(currentNodeSuffixKey, currentNode.value);
                    currentNode.key = currentNode.key.substring(0, j);
                    currentNode.value = value;
                    extendedNode.nodes = currentNode.nodes;
                    currentNode.nodes = new LinkedList<>();
                    currentNode.nodes.add(extendedNode);
                } else if (!suffixKey.isEmpty()) {
                    var foundEdge = false;
                    for (Node node : currentNode.nodes) {
                        if (node.key.substring(0, 1).equals(key.substring(i, i + 1))) {
                            nodesStack.clear();
                            nodesStack.add(node);
                            foundEdge = true;
                            break;
                        }
                    }
                    if (foundEdge) continue;

                    currentNode.nodes.add(new Node(suffixKey, value));
                }
                System.out.printf("prefix: %s, currentNode suffix: %s, new Node: suffix: %s\n", currentNode.key.substring(0, j), currentNodeSuffixKey, suffixKey);
                return;
            }

            nodesStack.addAll(currentNode.nodes);
        }
        root.nodes.add(new Node(key, value));
    }

    public Object find(String key) {
        if (root == null) {
            return null;
        }
        Queue<Node> nodesStack = new ArrayDeque<>(root.nodes);
        int i = 0;
        while (!nodesStack.isEmpty() && i < key.length()) {
            var currentNode = nodesStack.remove();
            System.out.printf("search prefix: %s, current node key: %s\n", key.substring(i), currentNode.key);

            int j = 0;
            while (i < key.length() && j < currentNode.key.length() && key.substring(i, i + 1).equals(currentNode.key.substring(j, j + 1))) {
                i++;
                j++;
            }

            if (j > 0) {

                if (i == key.length()) {
                    return currentNode.value;
                }
                nodesStack.clear();
                nodesStack.addAll(currentNode.nodes);
            }
        }

        return null;
    }

    public boolean delete(String key) {
        Queue<Node> nodesStack = new ArrayDeque<>();
        Stack<Node> parentsStack = new Stack<>();
        if (root == null) {
            return false;
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
            if (j > 0) {
                if (i == key.length()) {
                    currentNode.value = null;
                    var parent = parentsStack.pop();
                    var removed = parent.nodes.remove(currentNode);
                    System.out.println("deleting node: parent key: " + parent.key + "node is deleted: " + removed);

                    if (parent.nodes.size() == 1 && parent.value == null) {
                        var onlyChild = parent.nodes.remove();
                        System.out.printf("merging parent and child: parent key: %s, child key: %s\n", parent.key, onlyChild.key);
                        parent.key += onlyChild.key;
                        parent.value = onlyChild.value;

                    }

                    return true;
                }
                parentsStack.add(currentNode);
                nodesStack.addAll(currentNode.nodes);
            }
        }

        return false;
    }

    public void printTree() {
        printTree(root, "");
    }

    public void printTree(Node node, String prefix) {

        for (Node n : node.nodes) {
            printTree(n, prefix + node.key);
        }
        if (node.value == null && !node.nodes.isEmpty()) {
            return;
        }
        System.out.println(prefix + node.key + " | value: " + node.value);
        System.out.println("*************************");
    }
}
