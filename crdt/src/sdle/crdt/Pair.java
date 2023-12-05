package sdle.crdt;

import java.io.Serializable;
import java.util.Objects;

public class Pair<F extends Comparable<F>,S extends Comparable<S>> implements Serializable {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) obj;

        if (!Objects.equals(first, pair.first)) return false;
        return Objects.equals(second, pair.second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public int compareTo(Pair<F, S> other) {
        if (other.first.equals(this.first)) {
            return this.second.compareTo(other.second);
        } else {
            return this.first.compareTo(other.first);
        }
    }
}
