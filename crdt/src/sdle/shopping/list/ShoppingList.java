package sdle.shopping.list;

import sdle.crdt.AWORMap;
import sdle.crdt.AWORSet;
import sdle.crdt.CCounter;

import java.util.Map;
import java.util.Set;

public class ShoppingList {

    private AWORMap<String, CCounter<Integer, String>> shoppingList;

    public ShoppingList() {
        shoppingList = new AWORMap<>();
    }

    public void addItem(String item, int quantity) {
        CCounter<Integer, String> counter = (CCounter<Integer, String>) shoppingList.get(item);
        if (counter == null) {
            counter = new CCounter<>(item);
        }
        counter.increment(quantity);
        shoppingList.put(item, counter);
    }

    public void removeItem(String item, int quantity) {
        Set<CCounter<Integer,String>> counter = shoppingList.get(item);

        // get the first counter
        CCounter<Integer,String> firstCounter = counter.iterator().next();

        if (firstCounter != null) {
            firstCounter.decrement(quantity);
            if (firstCounter.readValue() == 0) {
                shoppingList.remove(item, firstCounter);
            } else {
                shoppingList.put(item, firstCounter);
            }
        }
    }

    public void displayShoppingList() {
        System.out.println("Shopping List:");
        for (Map.Entry<String, AWORSet<String, CCounter<Integer, String>>> entry : shoppingList.map.entrySet()) {
            String item = entry.getKey();
            AWORSet<String, CCounter<Integer, String>> counter = entry.getValue();
            StringBuilder output = new StringBuilder();
            output.append("\t").append(item).append(": ");
            for (CCounter<Integer, String> c : counter.read()) {
                output.append(c.readValue()).append(" ");
            }
            System.out.println(output);
        }
    }

    public void mergeShoppingList(AWORMap<String, CCounter<Integer, String>> otherList) {
        shoppingList.join(otherList);
    }

    public static void main(String[] args) {
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.addItem("Apple", 3);
        shoppingList.addItem("Orange", 3);
        shoppingList.addItem("Banana", 4);
        shoppingList.removeItem("Apple", 1);
        shoppingList.displayShoppingList();


    }




}
