package crdts;

import crdts.CCounter;
import crdts.Joinable;

import java.io.Serializable;

public class CCounterJoin implements Joinable<CCounter<Integer, String>>, Serializable {

    @Override
    public CCounter<Integer, String> join(CCounter<Integer, String> counter1, CCounter<Integer, String> counter2) {
        if (counter1 == null) return counter2;
        if (counter2 == null) return counter1;

        // Assuming CCounter has a method to get its current value and to set a new value

        int combinedValue = counter1.readValue() + counter2.readValue();
        CCounter<Integer, String> combinedCounter = new CCounter<>(counter1.getId());
        combinedCounter.setValue(combinedValue);

        return combinedCounter;
    }
}
