package crdts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;


public class DotContext<K extends Comparable<K>> implements Serializable {

    public Map<K, Integer> causalContext;
    public Set<Pair<K, Integer>> dotCloud;

    // Constructor
    public DotContext() {
        this.causalContext = new HashMap<>();
        this.dotCloud = new HashSet<>();
    }

    public DotContext(DotContext<K> other){
        this.causalContext = new HashMap<>(other.causalContext);
        this.dotCloud = new HashSet<>(other.dotCloud);
    }

    public DotContext<K> assign(DotContext<K> other){
        if (other == this) return this;

        causalContext.clear();
        causalContext.putAll(other.causalContext);

        dotCloud.clear();
        dotCloud.addAll(other.dotCloud);

        return this;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Context:");
        output.append(" CC ( ");

        for (Map.Entry<K, Integer> entry : causalContext.entrySet()) {
            output.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }

        output.append(")");

        output.append(" DC ( ");
        for (Pair<K, Integer> pair : dotCloud) {
            output.append(pair.getFirst()).append(":").append(pair.getSecond()).append(" ");
        }

        output.append(")");

        return output.toString();
    }

    public boolean dotIn(Pair<K, Integer> d) {
        Integer causalContextValue = causalContext.get(d.getFirst());

        if (causalContextValue != null && d.getSecond() <= causalContextValue) {
            return true;
        }

        return dotCloud.contains(d);
    }

    public void compact() {
        boolean flag;
        int i = 0;
        Iterator<Pair<K, Integer>> sit = dotCloud.iterator();
        do {
            flag = false;

            Pair<K, Integer> entry = null;
            if (i == 0) {
                if(sit.hasNext()){
                    entry = sit.next();
                    i++;
                } else {
                    entry = null;
                }
            }

            while(entry != null) {
                if(!causalContext.containsKey(entry.getFirst())) {
                    if(entry.getSecond() == 1){
                        causalContext.put(entry.getFirst(), entry.getSecond());
                        sit.remove();
                        if(!sit.hasNext()) entry = null;
                        else entry = sit.next();
                        flag = true;
                    } else {
                        if (sit.hasNext()) {
                            entry = sit.next();
                        } else {
                            entry = null;
                        }
                    }
                } else {
                    if(entry.getSecond() == causalContext.get(entry.getFirst()) + 1) {
                        causalContext.put(entry.getFirst(), causalContext.get(entry.getFirst()) + 1);
                        sit.remove();
                        if(!sit.hasNext()) entry = null;
                        else entry = sit.next();
                        flag = true;
                    } else if (entry.getSecond() <= causalContext.get(entry.getFirst())) {
                        sit.remove();
                        if(!sit.hasNext()) entry = null;
                        else entry = sit.next();
                    } else {
                        if (sit.hasNext()) {
                            entry = sit.next();
                        } else {
                            entry = null;
                        }
                    }
                }
            }
        } while(flag);
    }

    public Pair<K, Integer> makeDot(K id) {

        // check if id is in causal context
        if (causalContext.containsKey(id)) {
            causalContext.put(id, causalContext.get(id) + 1);
            return new Pair<K, Integer>(id, causalContext.get(id));
        }
        causalContext.put(id, 1);
        return new Pair<K, Integer>(id, 1);

    }

    public void insertDot(Pair<K, Integer> d, boolean compactNow) {
        // Set
        dotCloud.add(d);
        if (compactNow) {
            compact();
        }
    }

    public void join(DotContext<K> o) {
        if (this == o) return; // Join is idempotent, but just don't do it.

        // CC
        Iterator<Map.Entry<K, Integer>> mit = causalContext.entrySet().iterator();
        Iterator<Map.Entry<K, Integer>> mito = o.causalContext.entrySet().iterator();

        Map.Entry<K, Integer> mitNext = mit.hasNext() ? mit.next() : null;
        Map.Entry<K, Integer> mitoNext = mito.hasNext() ? mito.next() : null;

        while (mitNext != null || mitoNext != null) {
            if (mitNext != null && (mitoNext == null || mitNext.getKey().compareTo(mitoNext.getKey()) < 0)) {
                // entry only at this
                mitNext = mit.hasNext() ? mit.next() : null;
            } else if (mitoNext != null && (mitNext == null || mitoNext.getKey().compareTo(mitNext.getKey()) < 0)) {
                // entry only at other
                causalContext.put(mitoNext.getKey(), mitoNext.getValue());
                mitoNext = mito.hasNext() ? mito.next() : null;
            } else if (mitNext != null && mitoNext != null) {
                // entries at both
                causalContext.put(mitNext.getKey(), Math.max(mitNext.getValue(), mitoNext.getValue()));
                mitNext = mit.hasNext() ? mit.next() : null;
                mitoNext = mito.hasNext() ? mito.next() : null;
            }
        }

        // DC
        // Set
        for (Pair<K, Integer> e : o.dotCloud) {
            insertDot(e, false);
        }

        compact();
    }

    public static void main(String[] args) {
        // Test Case 1: Basic DotContext Operations
        DotContext<String> dotContext1 = new DotContext<>();
        Pair<String, Integer> dotA1 = dotContext1.makeDot("A");
        Pair<String, Integer> dotB1 = dotContext1.makeDot("B");

        System.out.println("Test Case 1: Basic DotContext Operations");
        System.out.println("DotContext 1 after making dots: " + dotContext1);
        System.out.println();

        // Test Case 2: Joining DotContexts
        DotContext<String> dotContext2 = new DotContext<>();
        Pair<String, Integer> dotC1 = dotContext2.makeDot("C");
        Pair<String, Integer> dotD1 = dotContext2.makeDot("D");

        dotContext1.join(dotContext2);

        System.out.println("Test Case 2: Joining DotContexts");
        System.out.println("DotContext 1 after joining with DotContext 2: " + dotContext1);
        System.out.println();

        // Test Case 3: Dot Inclusion Check
        Pair<String, Integer> dotE2 = new Pair<>("E", 2);
        System.out.println("Test Case 3: Dot Inclusion Check");
        System.out.println("Is dot (E, 2) in DotContext 1? " + dotContext1.dotIn(dotE2));
        System.out.println();

        // Test Case 4: Inserting a New Dot
        Pair<String, Integer> dotF1 = dotContext1.makeDot("F");
        dotContext1.insertDot(dotF1, true);

        System.out.println("Test Case 4: Inserting a New Dot");
        System.out.println("DotContext 1 after inserting a new dot: " + dotContext1);
        System.out.println();

        // Test Case 5: Compacting DotCloud
        dotContext1.compact();

        System.out.println("Test Case 5: Compacting DotCloud");
        System.out.println("DotContext 1 after compacting: " + dotContext1);
        System.out.println();

        // Test Case 6: Creating a New DotContext and Joining
        DotContext<String> dotContext3 = new DotContext<>();
        Pair<String, Integer> dotG1 = dotContext3.makeDot("G");
        Pair<String, Integer> dotH1 = dotContext3.makeDot("H");

        dotContext1.join(dotContext3);

        System.out.println("Test Case 6: Creating a New DotContext and Joining");
        System.out.println("DotContext 1 after joining with DotContext 3: " + dotContext1);
        System.out.println();
    }



}
