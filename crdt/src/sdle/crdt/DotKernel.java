package sdle.crdt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sdle.crdt.DotContext;

public class DotKernel<K extends Comparable<K>, T extends Comparable<T>> {
    Map<Pair<K, Integer>, T> dotMap;

    DotContext<K> cbase;
    DotContext<K> c;

    public DotKernel() {
        this.c = this.cbase = new DotContext<K>();
        this.dotMap = new HashMap<>();
    }

    public DotKernel(DotContext<K> jointc) {
        this.c = jointc;
        this.dotMap = new HashMap<>();
    }

    // Copy constructor
    public DotKernel(DotKernel<K, T> other){
        this.c = other.c;
        this.dotMap = new HashMap<>(other.dotMap);
    }

    public DotKernel<K, T> assign(DotKernel<K, T> other){
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
        Iterator<Map.Entry<Pair<T, Integer>, K>> ito = o.dotMap.entrySet().iterator();

        while(it.hasNext() || ito.hasNext()) {
            if(it.hasNext() && (ito.hasNext() && it.next().getKey().compareTo(ito.next().getKey() ) < 0)) {
                Map.Entry<Pair<K, Integer>, T> entry = it.next();
                if(o.c.dotIn(entry.getKey())) {
                    it.remove();
                } else if (ito.hasNext() && (it.hasNext() && ito.next().getKey().compareTo(it.next().getKey()) < 0)) {
                    Map.Entry<Pair<K, Integer>, T> entry = ito.next();
                    if (!c.dotIn(entry.getKey())) {
                        // If I don't know, import
                        dotMap.put(entry.getKey(), entry.getValue());
                    }
                } else if (it.hasNext() && ito.hasNext()) {
                    // dot in both
                    it.next();
                    ito.next();
                }

            }
        }

        c.join(o.c);


    }


}
