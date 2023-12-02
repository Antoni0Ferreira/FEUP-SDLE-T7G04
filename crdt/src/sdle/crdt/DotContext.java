package sdle.crdt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;


public class DotContext<K extends Comparable<K>> {

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
        do {
            flag = false;
            Iterator<Pair<K, Integer>> sit = dotCloud.iterator();
            while(sit.hasNext()) {
                Pair<K, Integer> pair = sit.next();
                if(!causalContext.containsKey(pair.getFirst())) {
                    if(pair.getSecond() == 1){
                        causalContext.put(pair.getFirst(), pair.getSecond());
                        sit.remove();
                        flag = true;
                    } else {
                        sit.next();
                    }
                } else {
                    if(pair.getSecond() == causalContext.get(pair.getFirst()) + 1) {
                        causalContext.put(pair.getFirst(), causalContext.get(pair.getFirst()) + 1);
                        sit.remove();
                        flag = true;
                    } else if (pair.getSecond() <= causalContext.get(pair.getFirst())) {
                        sit.remove();
                    } else {
                        sit.next();
                    }
                }
            }
        } while(flag);
    }

    public Pair<K, Integer> makeDot(K id) {
        // On a valid dot generator, all dots should be compact on the used id
        // Making the new dot updates the dot generator and returns the dot
        var kib = causalContext.put(id, 1);
        if(kib != null){
            causalContext.put(id, kib + 1);
        }
        return new Pair<K, Integer>(id, causalContext.get(id));
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

        do {
            boolean mitHasNext = mit.hasNext();
            boolean mitoHasNext = mito.hasNext();
            Map.Entry<K, Integer> mitNext = mit.hasNext() ? mit.next() : null;
            Map.Entry<K, Integer> mitoNext = mito.hasNext() ? mito.next() : null;
            if (mitHasNext && (!mitoHasNext || mitNext.getKey().compareTo(mitoNext.getKey()) < 0)) {
                // entry only at other
                // access previous iterator
                mit.next();
            }
            else if (mitoHasNext && (!mitHasNext || mitoNext.getKey().compareTo(mitNext.getKey()) < 0)) {
                mito.next();
                causalContext.put(mitoNext.getKey(), mitoNext.getValue());
            }
            else if (mitHasNext && mitoHasNext){
                causalContext.put(mitNext.getKey(), Math.max(mitNext.getValue(), mitoNext.getValue()));
                mit.next();
                mito.next();
            }
        } while (mit.hasNext() || mito.hasNext());

        // DC
        // Set
        for (Pair<K, Integer> e : o.dotCloud) {
            insertDot(e, false);
        }

        compact();
    }


}
