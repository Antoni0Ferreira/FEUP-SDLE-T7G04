package sdle.crdt;

public class NumericJoin implements Joinable<Integer> {
    @Override
    public Integer join(Integer value1, Integer value2) {
        return value1 + value2;
    }
}
