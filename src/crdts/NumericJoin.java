package crdts;

import java.io.Serializable;

public class NumericJoin implements Joinable<Integer>, Serializable {
    @Override
    public Integer join(Integer value1, Integer value2, Integer prev) {
        return prev + value1 + value2;
    }
}
