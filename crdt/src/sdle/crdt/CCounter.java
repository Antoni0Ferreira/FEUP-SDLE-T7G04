package sdle.crdt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CCounter<V extends Comparable<V>, K extends Comparable<K>> implements Comparable<CCounter<V, K>> {

    private K id;
    private Joinable<V> joinable = (Joinable<V>) new NumericJoin();
    private DotKernel<V, K> dotKernel = new DotKernel<>(joinable);


    public CCounter() {}

    public CCounter(K id) {
        this.id = id;
    }

    public CCounter(K id, DotContext<K> jointc) {
        this.id = id;
        this.dotKernel = new DotKernel<>(jointc, joinable);
    }

    public CCounter(CCounter<V, K> other) {
        this.id = other.id;
        this.dotKernel = new DotKernel<>(other.dotKernel, joinable);
    }

    public DotContext<K> getContext() {
        return dotKernel.c;
    }

    public K getId() {
        return id;
    }

    public void toString(StringBuilder output) {
        output.append("Casual Counter: ").append(id).append(" = ").append(dotKernel.toString());
    }

    public CCounter<V,K> increment(V value) {
        CCounter<V, K> result = new CCounter<>();
        Set<Pair<K, Integer>> dots = new HashSet<>();
        V base = null;

        for (Map.Entry<Pair<K, Integer>, V> it: dotKernel.dotMap.entrySet()) {
            System.out.println(it.getKey().getFirst());
            if(it.getKey().getFirst().equals(id)) {
                base = max(base, it.getValue());
                dots.add(it.getKey());
            }
        }

        for(Pair<K, Integer> dot: dots) {
            result.dotKernel.join(dotKernel.remove(dot));
        }

        result.dotKernel.join(dotKernel.add(id, add(base, value)));
        return result;
    }

    public CCounter<V,K> decrement(V value) {
        CCounter<V, K> result = new CCounter<>();
        Set<Pair<K, Integer>> dots = new HashSet<>();

        V base = null;

        for (Map.Entry<Pair<K, Integer>, V> it: dotKernel.dotMap.entrySet()) {
            if(it.getKey().getFirst().equals(id)) {
                base = max(base, it.getValue());
                dots.add(it.getKey());
            }
        }

        for(Pair<K, Integer> dot: dots) {
            result.dotKernel.join(dotKernel.remove(dot));
        }
        var newValue = sub(base, value);
        result.dotKernel.join(dotKernel.add(id, newValue));
        return result;

    }

    public CCounter<V, K> reset(){
        CCounter<V, K> result = new CCounter<>();
        result.dotKernel = dotKernel.removeAll();
        return result;
    }

    public V readValue() {
        V value = null;
        for (Map.Entry<Pair<K, Integer>, V> it : dotKernel.dotMap.entrySet()) {
            value = add(value, it.getValue());
        }

        if (value == null) {
            return (V) (Integer) 0;
        }
        return value;
    }

    public void join(CCounter<V, K> other) {
        dotKernel.join(other.dotKernel);
    }

    private V add(V base, V value) {
        if (value instanceof Integer) {
            if (base == null) {
                return value;
            }
            return (V) (Integer) ((Integer) base + (Integer) value);
        }
        return null;
    }


    private V sub(V base, V value) {
        if (value instanceof Integer) {
            if (base == null) {
                return (V) (Integer) (-1 * (Integer) value);
            }
            return (V) (Integer) ((Integer) base - (Integer) value);
        }
        return null;
    }

    private V max(V base, V value) {
        if (value instanceof Integer) {
            if (base == null) {
                return (V) value;
            }
            return (V) (Integer) Math.max((Integer) base, (Integer) value);
        }
        return null;
    }

    public static void main(String[] args) {

        // Test Case 1: Basic Increment and Read Value
        System.out.println("Test Case 1: Basic Increment and Read Value");
        CCounter<Integer, String> counter1 = new CCounter<>("Counter1");
        counter1.increment(5);
        System.out.println("Counter1 after incrementing by 5: " + counter1.readValue());

        //System.out.println("==================================================");

        // Test Case 2: Decrementing the Counter
        CCounter<Integer, String> counter2 = new CCounter<>("Counter2");
        counter2.increment(10);
        System.out.println("Counter2 before incrementing by 10: " + counter2.readValue());
        counter2.decrement(3);
        System.out.println("Counter2 after decrementing by 3: " + counter2.readValue());

        //System.out.println("==================================================");

        // Test Case 3: Resetting the Counter
        CCounter<Integer, String> counter3 = new CCounter<>("Counter3");
        counter3.increment(15);
        System.out.println("Counter3 before reset: " + counter3.readValue());
        counter3 = counter3.reset();
        System.out.println("Counter3 after reset: " + counter3.readValue());

        System.out.println("==================================================");

        // Test Case 4: Joining Two Counters
        CCounter<Integer, String> counter4 = new CCounter<>("Counter4");
        CCounter<Integer, String> counter5 = new CCounter<>("Counter5");

        counter4.increment(7);
        counter5.increment(5);
        System.out.println("Counter4 before join: " + counter4.readValue());
        System.out.println("Counter5 before join: " + counter5.readValue());

        counter4.join(counter5);
        System.out.println("Counter4 after joining with Counter5: " + counter4.readValue());

        System.out.println("==================================================");

        // Test Case 5: Complex Operations on a Single Counter
        CCounter<Integer, String> counter6 = new CCounter<>("Counter6");
        counter6.increment(20);
        counter6.decrement(5);
        counter6.increment(10);
        System.out.println("Counter6 after various operations: " + counter6.readValue());
        counter6 = counter6.reset();
        System.out.println("Counter6 after reset: " + counter6.readValue());

        System.out.println("==================================================");

        // Test Case 6: Joining Multiple Counters with Different Operations
        CCounter<Integer, String> counterA = new CCounter<>("CounterA");
        CCounter<Integer, String> counterB = new CCounter<>("CounterB");

        counterA.increment(10);
        counterB.decrement(5);

        System.out.println("CounterA before join: " + counterA.readValue());
        System.out.println("CounterB before join: " + counterB.readValue());

        counterA.join(counterB);

        System.out.println("CounterA after joining with CounterB: " + counterA.readValue());
        System.out.println("CounterB remains: " + counterB.readValue());

        System.out.println("==================================================");

        // Test Case 7: Resetting One Counter After Joining
        CCounter<Integer, String> counterC = new CCounter<>("CounterC");
        CCounter<Integer, String> counterD = new CCounter<>("CounterD");

        counterC.increment(7);
        counterD.increment(3);

        counterC.join(counterD);

        System.out.println("CounterC before reset: " + counterC.readValue());
        System.out.println("CounterD before reset: " + counterD.readValue());

        counterC = counterC.reset();

        System.out.println("CounterC after reset: " + counterC.readValue());
        System.out.println("CounterD remains: " + counterD.readValue());

        System.out.println("==================================================");

        // Test Case 8: Complex Join and Reset Operations Involving Multiple Counters
        CCounter<Integer, String> counterE = new CCounter<>("CounterE");
        CCounter<Integer, String> counterF = new CCounter<>("CounterF");
        CCounter<Integer, String> counterG = new CCounter<>("CounterG");

        counterE.increment(15);
        counterF.increment(10);
        counterG.decrement(5);

        counterE.join(counterF);
        counterG.join(counterE);

        System.out.println("CounterE after joining with F: " + counterE.readValue());
        System.out.println("CounterF remains: " + counterF.readValue());
        System.out.println("CounterG after joining with E: " + counterG.readValue());

        counterF.decrement(2);
        counterG = counterG.reset();

        System.out.println("CounterF after decrement: " + counterF.readValue());
        System.out.println("CounterG after reset: " + counterG.readValue());

        System.out.println("==================================================");

        // Test Case 9: Incrementing and Decrementing Multiple Counters Before Joining
        CCounter<Integer, String> counterH = new CCounter<>("CounterH");
        CCounter<Integer, String> counterI = new CCounter<>("CounterI");

        counterH.increment(20);
        counterI.decrement(10);

        counterH.join(counterI);

        System.out.println("CounterH after joining with I: " + counterH.readValue());
        System.out.println("CounterI remains: " + counterI.readValue());

        System.out.println("==================================================");

        // Test Case 10: Multiple Joins and Resets in Sequence
        CCounter<Integer, String> counterJ = new CCounter<>("CounterJ");
        CCounter<Integer, String> counterK = new CCounter<>("CounterK");
        CCounter<Integer, String> counterL = new CCounter<>("CounterL");

        counterJ.increment(5);
        counterK.increment(15);
        counterL.increment(10);

        counterJ.join(counterK);
        counterL.join(counterJ);

        System.out.println("CounterJ after multiple joins: " + counterJ.readValue());
        System.out.println("CounterK remains: " + counterK.readValue());
        System.out.println("CounterL after joining J: " + counterL.readValue());

        counterK = counterK.reset();
        counterL.decrement(5);

        System.out.println("CounterK after reset: " + counterK.readValue());
        System.out.println("CounterL after decrement: " + counterL.readValue());

    }

    @Override
    public int compareTo(CCounter<V, K> o) {
        return readValue().compareTo(o.readValue());
    }

    public void setValue(V combinedValue) {
        dotKernel = new DotKernel<>(joinable);
        dotKernel.join(dotKernel.add(id, combinedValue));
    }
}
