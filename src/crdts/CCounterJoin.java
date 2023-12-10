package crdts;

import crdts.CCounter;
import crdts.Joinable;

import java.io.Serializable;

public class CCounterJoin implements Joinable<CCounter<Integer, String>>, Serializable {

    @Override
    public CCounter<Integer, String> join(CCounter<Integer, String> counter1, CCounter<Integer, String> counter2,
                                          CCounter<Integer, String> prevCounter1) {
        if (counter1 == null) return counter2;
        if (counter2 == null) return counter1;
        int combinedValue = 0;

        if(prevCounter1 == null) {
            combinedValue = (counter1.readValue() + counter2.readValue());
        } else {
            System.out.println("Prev Counter 1 value: " + prevCounter1.readValue());
            System.out.println("Counter 1 value: " + counter1.readValue());
            System.out.println("Counter 2 value: " + counter2.readValue());
            combinedValue = (counter1.readValue() - prevCounter1.readValue()) +
                    (counter2.readValue() - prevCounter1.readValue());
        }

        // Assuming CCounter has a method to get its current value and to set a new value

        System.out.println("Combined value: " + combinedValue);
        CCounter<Integer, String> combinedCounter = new CCounter<>(counter1.getId());
        combinedCounter.setValue(combinedValue);

        return combinedCounter;
    }
}
