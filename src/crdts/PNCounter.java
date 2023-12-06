package crdts;

import crdts.GCounter;


public class PNCounter {
    private GCounter posCounter;
    private GCounter negCounter;
    private String id;

    public PNCounter(String id) {
        this.id = id;
        this.posCounter = new GCounter(id);
        this.negCounter = new GCounter(id);
    }

    public PNCounter increment(int sumValue) {
        this.posCounter = posCounter.increment(sumValue);
        return this;
    }

    public PNCounter increment() {
        this.posCounter = posCounter.increment();
        return this;
    }

    public PNCounter decrement(Integer sumValue) // Argument is optional
    {
        this.negCounter = negCounter.increment(sumValue);
        return this;
    }

    public PNCounter decrement() // Argument is optional
    {
        this.negCounter = negCounter.increment();
        return this;
    }

    public Integer localValue() {
        return this.posCounter.localValue() - this.negCounter.localValue();
    }

    public Integer readValue() // get counter value
    {
        Integer res = posCounter.readValue() - negCounter.readValue();
        return res;
    }

    public void merge(PNCounter other) {
        this.posCounter.merge(other.posCounter);
        this.negCounter.merge(other.negCounter);
    }

    public String toString() {
        StringBuilder result = new StringBuilder("PNCounter : ").append(this.id).append(" : ").append(this.posCounter.toString()).append(" ) - ( ").append(this.negCounter.toString()).append(" )");
        return result.toString();
    }
}
