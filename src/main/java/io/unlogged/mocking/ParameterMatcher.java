package io.unlogged.mocking;

public class ParameterMatcher {
    private final String name;
    private final String value;

    public ParameterMatcher(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ParameterMatcher{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
