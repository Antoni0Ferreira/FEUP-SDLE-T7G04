package sdle.crdt;

import jdk.jshell.Snippet;

import java.util.HashMap;
import java.util.Map;

public class AWORMap<E extends Comparable<E>, K extends Comparable<K>> {

    private Joinable<E> joinable = new Joinable<E>() {
        @Override
        public E join(E a, E b) {
            return a.compareTo(b) > 0 ? a : b;
        }
    };

    private DotKernel<E, K> dotKernel = new DotKernel<>(joinable);
    private final K id;

    public AWORMap(K id) {
        this.id = id;
    }

    public AWORMap(AWORMap<E, K> other) {
        this.id = other.id;
        this.dotKernel = new DotKernel<>(other.dotKernel, joinable);
    }

    public AWORMap(K id, DotContext<K> jointc) {
        this.id = id;
        this.dotKernel = new DotKernel<>(jointc, joinable);
    }

    public DotContext<K> getContext() {
        return dotKernel.c;
    }

    public String toString() {
        StringBuilder output = new StringBuilder("AWORMap: ");
        for (Map.Entry<Pair<K, Integer>, E> entry: dotKernel.dotMap.entrySet()) {
            Pair<K, Integer> key = entry.getKey();
            E value = entry.getValue();
            output.append(key.getFirst()).append(":").append(key.getSecond()).append(":").append(value).append(" ");
        }
        output.append(dotKernel.c.toString());
        return output.toString();
    }

    public Map<K, E> read() {
        Map<K, E> result = new HashMap<>();
        for(Map.Entry<Pair<K,Integer>, E> entry: dotKernel.dotMap.entrySet()) {
            Pair<K, Integer> key = entry.getKey();
            E value = entry.getValue();
            if(result.get(key.getFirst()) == null || result.get(key.getFirst()).compareTo(value) < 0) {
                result.put(key.getFirst(), value);
            }
        }
        return result;
    }

    public boolean containsKey(K key) {
        for(Map.Entry<Pair<K,Integer>, E> entry: dotKernel.dotMap.entrySet()) {
            Pair<K, Integer> k = entry.getKey();
            if(k.getFirst().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public AWORMap<E, K> add(K key, E val) {
        AWORMap<E, K> result = new AWORMap<>(this);
        Pair<K, Integer> highestDot = findHighestDot(key);
        if (highestDot != null && highestDot.getFirst().equals(id)) {
            result.dotKernel.dotAdd(key, val);
        } else {
            result.dotKernel.dotAdd(id, val);
        }
        return result;
    }

    public AWORMap<E, K> remove(K key, E val) {
        AWORMap<E, K> result = new AWORMap<>(this);
        Pair<K, Integer> highestDot = findHighestDot(key);

        if (highestDot != null && highestDot.getFirst().equals(id)) {
            result.dotKernel.remove(new Pair<>(key, highestDot.getSecond())); // Remove specific dot
        } else {
            result.dotKernel.remove(val); // Remove all occurrences of the value
        }

        return result;
    }

    AWORMap<E, K> reset() {
        AWORMap<E, K> result = new AWORMap<>(this);
        result.dotKernel = dotKernel.removeAll();
        return result;
    }

    public void join(AWORMap<E, K> other) {
        dotKernel.join(other.dotKernel);
    }

    private Pair<K, Integer> findHighestDot(K key) {
        Pair<K, Integer> highestDot = null;
        for(Map.Entry<Pair<K,Integer>, E> entry: dotKernel.dotMap.entrySet()) {
            Pair<K, Integer> k = entry.getKey();
            if(k.getFirst().equals(key)) {
                if(highestDot == null || highestDot.getSecond() < k.getSecond()) {
                    highestDot = k;
                }
            }
        }
        return highestDot;
    }

    public static void main(String[] args) {
        AWORMap<String, Integer> replica1 = new AWORMap<>(1);
        AWORMap<String, Integer> replica2 = new AWORMap<>(2);

        //replica1 = replica1.add("a", "1");
        //replica1 = replica1.add("a", "2");
    }

}
