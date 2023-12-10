package crdts;

public class JoinSelector {
    public static <T extends Comparable<T>> T join(boolean condition, T l, T r) {
        T res;
        if (condition) {
            res = max(l, r);
        } else {
            // Handle the case when the condition is false (you might want to do something else)
            res = l;
        }
        return res;
    }

    private static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static void main(String[] args) {
        System.out.println(join(true, 5, 2));
        System.out.println(join(false, 1, 2));
    }
}
