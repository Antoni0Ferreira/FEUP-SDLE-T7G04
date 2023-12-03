package sdle.crdt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CCounter<V extends Comparable<V>, K extends Comparable<K>> {

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

    public void toString(StringBuilder output) {
        output.append("Casual Counter: ").append(id).append(" = ").append(dotKernel.toString());
    }

    public CCounter<V,K> increment(V value) {
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
        result.dotKernel.join(dotKernel.add(id, sub(base, value)));
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
        return value;
    }

    public void join(CCounter<V, K> other) {
        dotKernel.join(other.dotKernel);
    }

    private V add(V base, V value) {
        if(value instanceof Integer) {
            if (base == null){
                return (V) value;
            }
            return (V) (Integer) ((Integer) base + (Integer) value);
        }
        return null;
    }

    private V sub(V base, V value) {
        if (value instanceof Integer) {
            if (base == null) {
                return (V) value;
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

}