package io.unlogged.test;

public class SimplePojoA {
    String aStringValue;
    Long aLongValue;

    public SimplePojoA(String aStringValue, Long aLongValue) {
        this.aStringValue = aStringValue;
        this.aLongValue = aLongValue;
    }

    public String getaStringValue() {
        return aStringValue;
    }

    public Long getaLongValue() {
        return aLongValue;
    }
}
