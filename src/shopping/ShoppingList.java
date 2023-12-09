package shopping;

import crdts.AWORMap;
import crdts.AWORSet;
import crdts.CCounter;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class ShoppingList implements Serializable {
    private final AWORMap<String, CCounter<Integer, String>> shoppingList;
    private Long id;
    public ShoppingList() {
        shoppingList = new AWORMap<>();
    }

    public ShoppingList(Long id) {
        shoppingList = new AWORMap<>();
        this.id = id;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AWORMap<String, CCounter<Integer, String>> getShoppingList() {
        return shoppingList;
    }

    public void addItem(String item, int quantity) {
        Set<CCounter<Integer, String>> counter = shoppingList.get(item);

        CCounter<Integer, String> firstCounter;
        if(counter != null) {
            firstCounter = counter.iterator().next();
        } else {
            firstCounter = new CCounter<>(item);
        }

        if(firstCounter.readValue() + quantity < 0) {
            firstCounter.decrement(firstCounter.readValue());
        } else {
            firstCounter.increment(quantity);
        }

        shoppingList.put(item, firstCounter);
    }

    public void removeItem(String item, int quantity) {
        Set<CCounter<Integer,String>> counter = shoppingList.get(item);

        // get the first counter
        CCounter<Integer,String> firstCounter;
        if(counter != null) {
            firstCounter = counter.iterator().next();
        } else {
            firstCounter = new CCounter<>();
        }

        // if the quantity to remove is greater than the quantity in the counter, remove the counter entirely
        if(firstCounter.readValue() - quantity < 0) {
            firstCounter.decrement(firstCounter.readValue());
        } else {
            firstCounter.decrement(quantity);
        }

        shoppingList.put(item, firstCounter);
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

        System.out.println("==================================================");

        ShoppingList shoppingList2 = new ShoppingList();
        shoppingList2.addItem("Apple", 5);
        shoppingList2.addItem("Orange", 2);
        shoppingList2.addItem("Banana", 2);
        shoppingList2.removeItem("Apple", 1);
        shoppingList2.displayShoppingList();

        System.out.println("==================================================");

        shoppingList.mergeShoppingList(shoppingList2.shoppingList);
        shoppingList.displayShoppingList();

        System.out.println("==================================================");

        System.out.println("Test Case: Removing an Item Completely");
        shoppingList.addItem("Milk", 2);
        shoppingList.removeItem("Milk", 2); // This should remove 'Milk' entirely from the list
        shoppingList.displayShoppingList();

        System.out.println("==================================================");

        System.out.println("Test Case: Attempting to Remove More Than Available");
        shoppingList.addItem("Eggs", 1);
        shoppingList.removeItem("Eggs", 2); // Attempting to remove more than the available quantity
        shoppingList.displayShoppingList();

        System.out.println("==================================================");

        System.out.println("Test Case: Adding an Item That Already Exists");
        shoppingList.addItem("Apple", 2);
        shoppingList.addItem("Apple", 3); // Incrementing the quantity of an existing item
        shoppingList.displayShoppingList();

        System.out.println("==================================================");






    }




}
