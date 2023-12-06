package crdts;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class AWORSet<K extends Comparable<K>, E extends Comparable<E>> {
     // Dot kernel
    private K id;
    private Joinable<E> joinable = (Joinable<E>) new CCounterJoin();
    private DotKernel<E, K> dotKernel = new DotKernel<>(joinable);

    public AWORSet() {
        // Only for deltas and those should not be mutated
    }

    public AWORSet(K k) {
        this.id = k;
        // Mutable replicas need a unique id
    }

    public AWORSet(K k, DotContext<K> jointc) {
        this.id = k;
        this.dotKernel = new DotKernel<>(jointc, joinable);
    }

    public DotContext<K> getContext() {
        return dotKernel.c;
    }

    @Override
    public String toString() {
        return "AWORSet:" + dotKernel.toString();
    }

    public Set<E> read() {
        Set<E> res = new HashSet<>();
        for (Map.Entry<Pair<K, Integer>, E> entry : dotKernel.dotMap.entrySet()) {
            res.add(entry.getValue());
        }
        return res;
    }

    public boolean in(E val) {
        for (Map.Entry<Pair<K, Integer>, E> entry : dotKernel.dotMap.entrySet()) {
            if (entry.getValue().equals(val))
                return true;
        }
        return false;
    }

    public AWORSet<K, E> add(E val) {
        AWORSet<K, E> r = new AWORSet<>(this.id);
        r.dotKernel = dotKernel.remove(val); // Optimization that first deletes val
        dotKernel.dotAdd(id, val);
        r.dotKernel.join(dotKernel);
        return r;
    }

    public AWORSet<K, E> remove(E val) {
        AWORSet<K, E> r = new AWORSet<>();

        var delta = dotKernel.remove(val);
        r.dotKernel = dotKernel;
        return r;
    }

    public AWORSet<K, E> reset() {
        AWORSet<K, E> r = new AWORSet<>();
        r.dotKernel = dotKernel.removeAll();
        return r;
    }

    public void join(AWORSet<K, E> o) {
        dotKernel.deepJoin(o.dotKernel);

    }

    public static void main(String[] args) {
        System.out.println("==================================================");

        // Test Case 1: Adding Elements to Set
        System.out.println("Test Case 1: Adding Elements to Set");
        AWORSet<String, Integer> set1 = new AWORSet<>("Set1");
        set1 = set1.add(1);
        set1 = set1.add(2);
        set1 = set1.add(3);
        System.out.println("Set1 after adding elements: " + set1);

        System.out.println("==================================================");

        // Test Case 2: Removing Element from Set
        System.out.println("Test Case 2: Removing Element from Set");
        AWORSet<String, Integer> set2 = new AWORSet<>("Set2");
        set2 = set2.add(1);
        set2 = set2.add(2);
        System.out.println("Set2 after adding elements: " + set2);
        set2 = set2.remove(1);
        System.out.println("Set2 after removing element: " + set2);

        System.out.println("==================================================");

        // Test Case 3: Checking Element Existence
        System.out.println("Test Case 3: Checking Element Existence");
        AWORSet<String, Integer> set3 = new AWORSet<>("Set3");
        set3 = set3.add(1);
        boolean exists = set3.in(1);
        System.out.println("Does 1 exist in Set3? " + exists);

        System.out.println("==================================================");

        // Test Case 4: Resetting the Set
        System.out.println("Test Case 4: Resetting the Set");
        AWORSet<String, Integer> set4 = new AWORSet<>("Set4");
        set4 = set4.add(1);
        set4 = set4.add(2);
        set4 = set4.reset();
        System.out.println("Set4 after reset: " + set4);

        System.out.println("==================================================");

        // Test Case 5: Joining Two Sets
        System.out.println("Test Case 5: Joining Two Sets");
        AWORSet<String, Integer> set5 = new AWORSet<>("Set5");
        AWORSet<String, Integer> set6 = new AWORSet<>("Set6");
        set5 = set5.add(1);
        set6 = set6.add(2);
        set5.join(set6);
        System.out.println("Set5 after joining with Set6: " + set5);

        System.out.println("==================================================");


        System.out.println("All test cases completed.");
    }
}
