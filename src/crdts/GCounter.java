package crdts;

import java.util.HashMap;
import java.util.Map;

public class GCounter {

    String id;
    Map<String, Integer> map;

    public GCounter(String id) {
        this.id = id;
        this.map = new HashMap<>();
    }

    public GCounter increment(int sumValue) {
        this.map.put(this.id, (this.map.get(this.id) == null ? 0 : this.map.get(this.id)) + sumValue);
        return this;
    }

    public GCounter increment() {
        this.map.put(this.id, (this.map.get(this.id) == null ? 0 : this.map.get(this.id)) + 1);
        return this;
    }

    public boolean equals(GCounter other) {
        return this.map.get(this.id) == other.map.get(this.id);
    }

    public int localValue() {
        int result = 0;
        result += (this.map.get(this.id) == null ? 0 : this.map.get(this.id));
        return result;
    }

    public int readValue() {
        int result = 0;
        for (String key : this.map.keySet()) {
            result += this.map.get(key);
        }
        return result;
    }

    public void merge(GCounter other) {
        for (String key : other.map.keySet()) {
            if(this.map.get(key) == null || this.map.get(key) < other.map.get(key)) {
                this.map.put(key, other.map.get(key));
            }
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder("GCounter: ( ");
        for (String key : this.map.keySet()) {
            result.append(key).append(": ").append(this.map.get(key)).append(", ");
        }
        result.append(")");
        return result.toString();
    }

    public void main(String[] args) {
        System.out.println("Hello world!");
    }
};



