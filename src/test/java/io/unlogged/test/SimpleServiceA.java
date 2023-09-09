package io.unlogged.test;

import java.util.Random;

public class SimpleServiceA {
    private SimpleServiceB simpleServiceB;

    public SimpleServiceA(SimpleServiceB simpleServiceB) {
        this.simpleServiceB = simpleServiceB;
    }

    public SimplePojoA callToTest(String aValue, SimplePojoB pojoB) {
        System.out.println("Call on [" + this.getClass() + "] callToTest");
        String aNewVal = aValue + "-991";
        SimplePojoA returnedValue = simpleServiceB.makeAndReturn(aNewVal, new SimplePojoB(pojoB.getStrValue() +
                "ok", pojoB.getIntValue() + 1));
        return returnedValue;
    }
}
