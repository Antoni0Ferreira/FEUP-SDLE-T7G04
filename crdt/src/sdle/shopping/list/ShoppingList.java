package sdle.shopping.list;

import sdle.crdt.AWORMap;
import sdle.crdt.CCounter;

import java.util.Map;

public class ShoppingList {

    private AWORMap<String, CCounter<Integer, String>> shoppingList;
    private String id;

    public ShoppingList(String id) {
        this.id = id;
        shoppingList = new AWORMap<>(this.id);
    }

    public void addItem(String item, int quantity) {
        CCounter<Integer, String> counter = (CCounter<Integer, String>) shoppingList.get(item);
        if (counter == null) {
            counter = new CCounter<>(this.id);
        }
        counter.increment(quantity);
        shoppingList.put(item, counter);
    }

    public void removeItem(String item, int quantity) {
        CCounter<Integer, String> counter = (CCounter<Integer, String>) shoppingList.get(item);
        if (counter != null) {
            counter.decrement(quantity);
            if (counter.readValue() == 0) {
                shoppingList.remove(item);
            } else {
                shoppingList.put(item, counter);
            }
        }
    }

    public void displayShoppingList() {
        for (Map.Entry<String, CCounter<Integer, String>> entry : shoppingList.entrySet()) {
            System.out.println("Item: " + entry.getKey() + ", Quantity: " + entry.getValue().getCurrentValue());
        }
    }

    public void mergeShoppingList(AWORMap<String, CCounter<Integer, String>> otherList) {
        shoppingList.join(otherList);
    }




}
