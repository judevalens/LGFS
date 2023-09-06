public class Main {
    public static void main(String[] args) {
        RTree tree = new RTree();

        tree.add("romanus",2);
        tree.add("romulus",3);
        tree.add("romane/Nero",-8);
        tree.add("rubicon",6);
        tree.add("rubic",100);
        tree.add("rubicundus",-6);
        tree.add("rom",10);
        tree.printTree();
    }
}
