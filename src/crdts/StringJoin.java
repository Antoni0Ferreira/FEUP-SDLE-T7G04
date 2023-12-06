package crdts;

public class StringJoin implements Joinable<String> {
    @Override
    public String join(String value1, String value2) {
        return value1 + value2;
    }
}
