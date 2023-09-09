package io.unlogged.test;

public class SimpleServiceB {


    public SimplePojoA makeAndReturn(String param1, SimplePojoB pojoB) {
        System.out.println("Call on [" + this.getClass() + "] makeAndReturn");
        SimplePojoA newPojoA = new SimplePojoA(param1, (long) (pojoB.getIntValue() + 5));
        return newPojoA;
    }

}
