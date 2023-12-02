import sdle.crdt.PNCounter;

public class Main {
    public static void main(String[] args) {

        System.out.println("Hello world!");
        PNCounter counter1 = new PNCounter("A");
        PNCounter counter2 = new PNCounter("B");

        counter1 = counter1.increment(5);
        counter2 = counter2.increment(3);
        System.out.println(counter1.toString());
        System.out.println(counter2.toString());

        counter1.merge(counter2);
        System.out.println(counter1);

        counter1 = counter1.decrement(2);
        counter2 = counter2.decrement(1);
        counter1.merge(counter2);
        System.out.println(counter1);

        System.out.println(counter1.readValue());

        counter1 = counter1.increment(5);
        System.out.println(counter1);
        System.out.println(counter1.readValue());
    }
}