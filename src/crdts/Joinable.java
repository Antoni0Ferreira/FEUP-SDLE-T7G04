package crdts;

public interface Joinable<T> {
    T join(T value1, T value2);
}
