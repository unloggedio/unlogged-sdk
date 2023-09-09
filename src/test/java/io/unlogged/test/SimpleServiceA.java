package io.unlogged.test;

public class SimpleServiceA {
    private SimpleServiceB simpleServiceB;

    public SimpleServiceA(SimpleServiceB simpleServiceB) {
        this.simpleServiceB = simpleServiceB;
    }

    public SimplePojoA callToTest(String aValue, SimplePojoB pojoB) {
        String aNewVal = aValue + "-991";
        SimplePojoA returnedValue = simpleServiceB.makeAndReturn(aNewVal, new SimplePojoB(pojoB.getStrValue() +
                "ok", pojoB.getIntValue() + 1));
        return returnedValue;
    }

    public SimplePojoA callToTest1(String aValue, SimplePojoB pojoB) {
        String aNewVal = aValue + "-991";
        SimplePojoA returnedValue = simpleServiceB.makeAndReturn1(aNewVal, new SimplePojoB(pojoB.getStrValue() +
                "ok", pojoB.getIntValue() + 1));
        return returnedValue;
    }
}
