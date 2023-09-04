public class Main {
    public static void main(String[] args) {
        RTree tree = new RTree();

        tree.add("Romane/",1);
        tree.add("Romane/Germanicus",-5);
        tree.add("Romane/Caesar",-7);
        tree.add("rubber",5);
        tree.add("rub",-1);
        tree.add("rub",-2);
        tree.add("Romanus",2);
        tree.add("Romulus",3);
        tree.add("Romane/Nero",-8);
        tree.add("rubicon",6);
        tree.add("rubicundus",-6);
        tree.add("Rom",10);
        tree.printTree();

        System.out.println(tree.find("/Rom"));
        System.out.println(tree.find("/Romane"));
        System.out.println(tree.find("/rub"));
        System.out.println(tree.find("/rubber"));
    }
}
