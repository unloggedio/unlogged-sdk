package io.unlogged;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

public class TestBaseImplementation {


    @RuntimeType
    @BindingPriority(value = 1000)
    public Object intercept(@AllArguments Object[] methodArguments,
                            @This Object thisInstance,
                            @Origin Method invokedMethod,
                            @Super Object superInstance
    ) {

        return "ok";
    }


}
