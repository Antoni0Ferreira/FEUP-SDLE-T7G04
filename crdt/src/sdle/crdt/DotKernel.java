package sdle.crdt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sdle.crdt.DotContext;

public class DotKernel<T extends Comparable<T>, K extends Comparable<K>> {
    Map<Pair<K, Integer>, T> dotMap;

    DotContext<K> cbase;
    DotContext<K> c;

    private Joinable<T> joinable;

    public DotKernel(Joinable<T> joinable) {
        this.c = this.cbase = new DotContext<K>();
        this.dotMap = new HashMap<>();
        this.joinable = joinable;
    }

    public DotKernel(DotContext<K> jointc, Joinable<T> joinable) {
        this.c = jointc;
        this.dotMap = new HashMap<>();
        this.joinable = joinable;
    }

    // Copy constructor
    public DotKernel(DotKernel<T, K> other, Joinable<T> joinable) {
        this.c = other.c;
        this.dotMap = new HashMap<>(other.dotMap);
        this.joinable = joinable;
    }

    public DotKernel<T, K> assign(DotKernel<T, K> other){
        if (other == this) return this;

        if(c != other.c){
            c = other.c;
        }

        dotMap = new HashMap<>(other.dotMap);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Kernel: DS ( ");
        for (Map.Entry<Pair<K, Integer>, T> entry: dotMap.entrySet()) {
            Pair<K, Integer> key = entry.getKey();
            T value = entry.getValue();
            output.append(key.getFirst()).append(":").append(key.getSecond()).append(":").append(value).append(" ");
        }
        output.append(")");
        output.append(c.toString());
        return output.toString();
    }

    public void join(DotKernel<T, K> o) {
        if (this == o) return;

        Iterator<Map.Entry<Pair<K, Integer>, T>> it = dotMap.entrySet().iterator();
        Iterator<Map.Entry<Pair<K, Integer>, T>> ito = o.dotMap.entrySet().iterator();

        do {

            if(it.hasNext() && (!ito.hasNext() || it.next().getKey().compareTo(ito.next().getKey()) < 0)) {
                Map.Entry<Pair<K, Integer>, T> entry = it.next();
                if(o.c.dotIn(entry.getKey())) {
                    it.remove();
                }
            }
            else if(ito.hasNext() && (!it.hasNext() || it.next().getKey().compareTo(ito.next().getKey()) > 0)) {
                Map.Entry<Pair<K, Integer>, T> entry = ito.next();
                if(!c.dotIn(entry.getKey())) {
                    dotMap.put(entry.getKey(), entry.getValue());
                }
            }
            else if(it.hasNext() && ito.hasNext()) {
                it.next();
                ito.next();
            }
        } while(it.hasNext() || ito.hasNext());

        c.join(o.c);
    }

    public void deepJoin(DotKernel<T,K> o){
        if(this == o) return;

        Iterator<Map.Entry<Pair<K, Integer>, T>> it = dotMap.entrySet().iterator();
        Iterator<Map.Entry<Pair<K, Integer>, T>> ito = o.dotMap.entrySet().iterator();

        do {
            if(it.hasNext() && (!ito.hasNext() || it.next().getKey().compareTo(ito.next().getKey()) < 0)) {
                Map.Entry<Pair<K, Integer>, T> entry = it.next();
                if(o.c.dotIn(entry.getKey())) {
                    it.remove();
                }
            }
            else if(ito.hasNext() && (!it.hasNext() || it.next().getKey().compareTo(ito.next().getKey()) > 0)) {
                Map.Entry<Pair<K, Integer>, T> entry = ito.next();
                if(!c.dotIn(entry.getKey())) {
                    dotMap.put(entry.getKey(), entry.getValue());
                }
            }
            else if(it.hasNext() && ito.hasNext()) {
                Map.Entry<Pair<K, Integer>, T> entry = it.next();
                Map.Entry<Pair<K, Integer>, T> entryo = ito.next();
                if(!entry.getValue().equals(entryo.getValue())) {
                    entry.setValue(join(entry.getValue(), entryo.getValue()));
                }

            }
        } while(it.hasNext() || ito.hasNext());

        c.join(o.c);
    }

    public DotKernel<T, K> add(K key, T value) {
        DotKernel<T, K> result = new DotKernel<>(joinable);
        Pair<K, Integer> dot = c.makeDot(key);

        dotMap.put(dot, value);

        // make delta
        result.dotMap.put(dot, value);
        result.c.insertDot(dot, true);
        return result;
    }

    public Pair<K, Integer> dotAdd(K key, T value) {
        Pair<K, Integer> dot = c.makeDot(key);
        dotMap.put(dot, value);

        return dot;
    }

    public DotKernel<T, K> remove(T val) {
        DotKernel<T, K> result = new DotKernel<>(joinable);
        Iterator<Map.Entry<Pair<K, Integer>, T>> it = dotMap.entrySet().iterator();

        while(it.hasNext()) {
            Map.Entry<Pair<K, Integer>, T> entry = it.next();
            if(entry.getValue().equals(val)) {
                result.c.insertDot(entry.getKey(), false);
                it.remove();
            }
        }

        result.c.compact();
        return result;
    }

    public DotKernel<T, K> remove(Pair<K, Integer> dot) {
        DotKernel<T, K> result = new DotKernel<>(joinable);

        Map.Entry<Pair<K, Integer>, T> entry = (Map.Entry<Pair<K, Integer>, T>) dotMap.get(dot);
        if(entry != null) {
            result.c.insertDot(entry.getKey(), false);
            dotMap.remove(dot);
        }

        result.c.compact();
        return result;

    }

    public DotKernel<T, K> removeAll() {
        DotKernel<T, K> result = new DotKernel<>(joinable);

        for (Map.Entry<Pair<K, Integer>, T> entry : dotMap.entrySet()) {
            result.c.insertDot(entry.getKey(), false);
        }

        result.c.compact();
        dotMap.clear();
        return result;
    }




    private T join(T value1, T value2) {
        return joinable.join(value1, value2);
    }


}
