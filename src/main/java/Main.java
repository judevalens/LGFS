public class Main {
    public static void main(String[] args) {
        RTree tree = new RTree();

        tree.add("Romane",1);
        tree.add("rubber",5);
        tree.add("rub",-1);
        tree.add("Romanus",2);
        tree.add("Romulus",3);
        tree.add("rubicon",6);
        tree.printTree();
    }
}
