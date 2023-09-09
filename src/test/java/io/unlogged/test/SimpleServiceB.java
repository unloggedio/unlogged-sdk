package io.unlogged.test;

public class SimpleServiceB {


    public SimplePojoA makeAndReturn(String param1, SimplePojoB pojoB) {
        SimplePojoA newPojoA = new SimplePojoA(param1, (long) (pojoB.getIntValue() + 5));
        return newPojoA;
    }


    public SimplePojoA makeAndReturn1(String param1, SimplePojoB pojoB) {
        SimplePojoA newPojoA = new SimplePojoA(param1, (long) (pojoB.getIntValue() + 5));
        return newPojoA;
    }

}
