package sdle.crdt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DotKernel<T extends Comparable<T>, K extends Comparable<K>> {
    Map<Pair<K, Integer>, T> dotMap;

    DotContext<K> cbase;
    DotContext<K> c;

    private final Joinable<T> joinable;

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
        StringBuilder output = new StringBuilder("Kernel: DS (");
        for (Map.Entry<Pair<K, Integer>, T> entry: dotMap.entrySet()) {
            Pair<K, Integer> key = entry.getKey();
            T value = entry.getValue();
            output.append("(").append(key.getFirst()).append(":").append(key.getSecond()).append(")->").append(value).append(" ");
        }
        output.append(")");
        output.append(c.toString());
        return output.toString();
    }

    public void join(DotKernel<T, K> o) {
        if (this == o) return;

        Iterator<Map.Entry<Pair<K, Integer>, T>> it = dotMap.entrySet().iterator();
        Iterator<Map.Entry<Pair<K, Integer>, T>> ito = o.dotMap.entrySet().iterator();
        int i = 0;
        Map.Entry<Pair<K, Integer>, T> entry = null;
        Map.Entry<Pair<K, Integer>, T> entryo = null;

        do {

            if(i == 0){
                entry = it.next();
                entryo = ito.next();
                i++;
            }

            if(entry != null && (entryo == null || entry.getKey().getFirst().compareTo(entryo.getKey().getFirst()) < 0)) {
                if(o.c.dotIn(entry.getKey())) {
                    it.remove();

                }
                if(it.hasNext()){
                    entry = it.next();
                }
                else{
                    entry = null;
                }
            }
            else if(entryo != null && (entry == null || entry.getKey().getFirst().compareTo(entryo.getKey().getFirst()) > 0)) {
                if(!c.dotIn(entryo.getKey())) {
                    dotMap.put(entryo.getKey(), entryo.getValue());

                }

                if(ito.hasNext()){
                    entryo = ito.next();
                }
                else{
                    entryo = null;
                }

            }
            else if(entry != null && entryo != null) {
                System.out.println("hello!");

                if(it.hasNext()){
                    entry = it.next();
                }
                else{
                    entry = null;
                }

                if(ito.hasNext()){
                    entryo = ito.next();
                }
                else{
                    entryo = null;
                }

            }
        } while(entry != null || entryo != null);

        c.join(o.c);
    }

    public void deepJoin(DotKernel<T,K> o){
        if(this == o) return;

        Iterator<Map.Entry<Pair<K, Integer>, T>> it = dotMap.entrySet().iterator();
        Iterator<Map.Entry<Pair<K, Integer>, T>> ito = o.dotMap.entrySet().iterator();
        int i = 0;
        Map.Entry<Pair<K, Integer>, T> entry = null;
        Map.Entry<Pair<K, Integer>, T> entryo = null;

        do {

            if(i == 0){
                entry = it.next();
                entryo = ito.next();
                i++;
            }

            if(entry != null && (entryo == null || entry.getKey().getFirst().compareTo(entryo.getKey().getFirst()) < 0)) {
                if(o.c.dotIn(entry.getKey())) {
                    it.remove();
                }

                if(ito.hasNext()){
                    entryo = ito.next();
                }
                else{
                    entryo = null;
                }
            }
            else if(entryo != null && (entry == null || entryo.getKey().getFirst().compareTo(entry.getKey().getFirst()) > 0)) {
                if(!c.dotIn(entryo.getKey())) {
                    dotMap.put(entryo.getKey(), entryo.getValue());
                }

                if(ito.hasNext()){
                    entryo = ito.next();
                }
                else{
                    entryo = null;
                }
            }
            else if(entry != null && entryo != null) {

                if(!entry.getValue().equals(entryo.getValue())) {
                    entry.setValue(join(entry.getValue(), entryo.getValue()));
                }

                if(it.hasNext()){
                    entry = it.next();
                }
                else{
                    entry = null;
                }

                if(ito.hasNext()){
                    entryo = ito.next();
                }
                else{
                    entryo = null;
                }

            }
        } while(entry != null || entryo != null);

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

    public void dotAdd(K key, T value) {
        Pair<K, Integer> dot = c.makeDot(key);
        dotMap.put(dot, value);

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

    public static void main(String[] args) {
        // Test Case 1: Basic DotKernel Operations
        Joinable<Integer> integerJoinable = Integer::sum;
        DotKernel<Integer, String> dotKernel1 = new DotKernel<>(integerJoinable);

        DotKernel<Integer, String> dotKernel2 = new DotKernel<>(integerJoinable);

        dotKernel1.dotAdd("A", 2);
        dotKernel2.dotAdd("A", 1);
        dotKernel2.dotAdd("B", 2);
        dotKernel2.dotAdd("A", 6);

        System.out.println("DotKernel 1: " + dotKernel1);
        System.out.println("DotKernel 2: " + dotKernel2);

        dotKernel1.deepJoin(dotKernel2);

        System.out.println("DotKernel 1: " + dotKernel1);
        System.out.println("DotKernel 2: " + dotKernel2);


        System.out.println();

    }



}
