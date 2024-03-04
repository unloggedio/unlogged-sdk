package io.unlogged.mocking;

import java.util.Objects;

public class ParameterMatcher {
    private String name;
    private ParameterMatcherType type;
    private String value;

    public ParameterMatcher(String name, ParameterMatcherType type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public ParameterMatcher(ParameterMatcher e) {
        this.name = e.name;
        this.type = e.type;
        this.value = e.value;
    }

    public ParameterMatcher() {
    }

    public ParameterMatcherType getType() {
        return type;
    }

    public void setType(ParameterMatcherType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ParameterMatcher{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterMatcher that = (ParameterMatcher) o;
        return Objects.equals(name, that.name) && type == that.type && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value);
    }
}
