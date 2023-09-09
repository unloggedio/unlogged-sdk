package io.unlogged.test;

public class SimplePojoA {
    String aStringValue;

    public SimplePojoA() {
    }

    public void setaStringValue(String aStringValue) {
        this.aStringValue = aStringValue;
    }

    public void setaLongValue(Long aLongValue) {
        this.aLongValue = aLongValue;
    }

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
