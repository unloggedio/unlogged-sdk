package io.unlogged.test;

public class ExtendedSimpleServiceA extends SimpleServiceA {
    public ExtendedSimpleServiceA(SimpleServiceB simpleServiceB) {
        super(simpleServiceB);
    }

    public SimplePojoA callToTest2(String aValue, SimplePojoB pojoB) {
        String aNewVal = aValue + "-991";
        SimplePojoA returnedValue = super.callToTest(aNewVal, new SimplePojoB(pojoB.getStrValue() +
                "ok", pojoB.getIntValue() + 1));
        return returnedValue;
    }

}
