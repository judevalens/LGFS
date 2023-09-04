import java.util.LinkedList;

public class Node {
    String key;
    LinkedList<Node> nodes;
    Object value;

    public Node(String key, Object value) {
        this.key = key;
        this.value = value;
        nodes = new LinkedList<>();
    }
}
