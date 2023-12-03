package sdle.crdt;

import java.util.HashMap;
import java.util.Map;

class RwCounter {
    private Map<String, Data> b = new HashMap<>();
    private String id;

    public RwCounter() {
        this.id = "";
    }

    public RwCounter(String id) {
        this.id = id;
    }

    public RwCounter(String id, Map<String, Data> jointc) {
        this.id = id;
        if (jointc != null) {
            this.b = new HashMap<>(jointc);
        }
    }

    public RwCounter operatorEquals(RwCounter o) {
        if (this == o) {
            return this;
        }
        if (!this.b.equals(o.b)) {
            this.b = new HashMap<>(o.b);
        }
        this.id = o.id;
        return this;
    }

    public Map<String, Data> context() {
        return this.b;
    }

    public RwCounter inc(int val) {
        RwCounter r = new RwCounter();
        Data data = this.b.getOrDefault(this.id, new Data());
        Data newData = new Data(data.first + val, data.second);
        this.b.put(this.id, newData);
        r.b.put(this.id, newData);
        return r;
    }

    public RwCounter dec(int val) {
        RwCounter r = new RwCounter();
        Data data = this.b.getOrDefault(this.id, new Data());
        Data newData = new Data(data.first, data.second + val);
        this.b.put(this.id, newData);
        r.b.put(this.id, newData);
        return r;
    }

    public RwCounter reset() {
        RwCounter r = new RwCounter();
        r.b.clear();
        return r;
    }

    public void fresh() {
        this.b.clear();
    }

    public int read() {
        Data ac = new Data(0, 0);
        for (Map.Entry<String, Data> entry : this.b.entrySet()) {
            Data value = entry.getValue();
            ac.first += value.first;
            ac.second += value.second;
        }
        return ac.first - ac.second;
    }

    public void join(RwCounter o) {
        for (Map.Entry<String, Data> entry : o.b.entrySet()) {
            String key = entry.getKey();
            Data value = entry.getValue();
            this.b.putIfAbsent(key, new Data(0, 0));
            Data existingData = this.b.get(key);
            existingData.first += value.first;
            existingData.second += value.second;
        }
    }

    @Override
    public String toString() {
        return "ResetWinsCounter: " + this.b.entrySet();
    }

    private static class Data {
        int first;
        int second;

        Data() {
            this.first = 0;
            this.second = 0;
        }

        Data(int first, int second) {
            this.first = first;
            this.second = second;
        }
        
    }

    public static void main(String[] args) {
        RwCounter rwc1 = new RwCounter("i");
        RwCounter rwc2 = new RwCounter("j");

        rwc1.inc(1);
        rwc1.inc(2);
        rwc1.dec(1);
        rwc2.inc(5);

        System.out.println(rwc1.toString());
        System.out.println(rwc2.toString());

        rwc1.join(rwc2);

        System.out.println(rwc1.toString());
        System.out.println(rwc1.read());
        System.out.println("Reset: " + rwc2.reset().toString());
        System.out.println("Delta: " + rwc2.inc(1).toString());

        rwc1.join(rwc2);

        System.out.println(rwc1.toString());
        System.out.println(rwc1.read());

        rwc2.join(rwc1);
        rwc2.reset();
        rwc1.fresh();

        System.out.println(rwc1.toString());

        rwc1.inc(1);

        System.out.println(rwc1.toString());

        rwc1.join(rwc2);

        System.out.println(rwc1.toString());
        System.out.println(rwc1.read());
    }
}
