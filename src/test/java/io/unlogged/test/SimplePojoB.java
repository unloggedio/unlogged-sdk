package io.unlogged.test;

public class SimplePojoB {
    String strValue;
    Integer intValue;

    public SimplePojoB(String strValue, Integer intValue) {
        this.strValue = strValue;
        this.intValue = intValue;
    }

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public SimplePojoB() {
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }
}
