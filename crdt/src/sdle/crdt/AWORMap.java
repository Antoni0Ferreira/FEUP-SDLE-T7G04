package sdle.crdt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AWORMap<K extends Comparable<K>, V extends Comparable<V>> {
    private Map<K, AWORSet<K, V>> map;

    public AWORMap() {
        map = new HashMap<>();
    }

    public void put(K key, V value) {
        AWORSet<K, V> set = map.getOrDefault(key, new AWORSet<>(key));
        set = set.add(value);
        map.put(key, set);
    }

    public void remove(K key, V value) {
        if (map.containsKey(key)) {
            AWORSet<K, V> set = map.get(key);
            set = set.remove(value);
            map.put(key, set);
        }
    }

    public Set<V> get(K key) {
        if (map.containsKey(key)) {
            return map.get(key).read();
        }
        return null; // Or return an empty set
    }

    public void join(AWORMap<K, V> other) {
        for (Map.Entry<K, AWORSet<K, V>> entry : other.map.entrySet()) {
            K key = entry.getKey();
            AWORSet<K, V> otherSet = entry.getValue();
            AWORSet<K, V> thisSet = map.getOrDefault(key, new AWORSet<>(key));
            thisSet.join(otherSet);
            map.put(key, thisSet);
        }
    }

    // For testing purposes
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public boolean containsKeyValue(K key, V value) {
        return map.containsKey(key) && map.get(key).in(value);
    }

    @Override
    public String toString() {
        return "AWORMap{" + "map=" + map + '}';
    }

    // Main method for testing
    public static void main(String[] args) {
        System.out.println("==================================================");

        // Test Case 1: Adding Elements to Map
        System.out.println("Test Case 1: Adding Elements to Map");
        AWORMap<String, Integer> map1 = new AWORMap<>();
        map1.put("fruit", 1);
        map1.put("fruit", 2);
        map1.put("vegetable", 3);
        System.out.println("Map1 after adding elements: " + map1);

        System.out.println("==================================================");

        // Test Case 2: Removing Element from Map
        System.out.println("Test Case 2: Removing Element from Map");
        AWORMap<String, Integer> map2 = new AWORMap<>();
        map2.put("fruit", 1);
        map2.put("fruit", 2);
        map2.remove("fruit", 1);
        System.out.println("Map2 after removing element: " + map2);

        System.out.println("==================================================");

        // Test Case 3: Resetting the Map
        System.out.println("Test Case 3: Resetting the Map");
        AWORMap<String, Integer> map3 = new AWORMap<>();
        map3.put("fruit", 1);
        map3.put("fruit", 2);
        map3 = new AWORMap<>(); // Reset by reinitializing
        System.out.println("Map3 after reset: " + map3);

        System.out.println("==================================================");

        // Test Case 4: Joining Two Maps
        System.out.println("Test Case 4: Joining Two Maps");
        AWORMap<String, Integer> map4 = new AWORMap<>();
        AWORMap<String, Integer> map5 = new AWORMap<>();
        map4.put("fruit", 1);
        map5.put("vegetable", 3);
        map4.join(map5);
        System.out.println("Map4 after joining with Map5: " + map4);

        System.out.println("==================================================");

        // Test Case 5: Complex Operations on a Single Map
        System.out.println("Test Case 5: Complex Operations on a Single Map");
        AWORMap<String, Integer> map6 = new AWORMap<>();
        map6.put("fruit", 1);
        map6.put("fruit", 2);
        map6.remove("fruit", 1);
        System.out.println("Map6 after various operations: " + map6);
        map6 = new AWORMap<>(); // Reset
        System.out.println("Map6 after reset: " + map6);

        System.out.println("==================================================");

        // Additional test cases can be added following the same structure,
        // involving more complex scenarios and combinations of operations.

        System.out.println("All test cases completed.");
    }
}
