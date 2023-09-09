package io.unlogged.mocking;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class MockHandler {
    private final List<DeclaredMock> declaredMocks;

    public MockHandler(List<DeclaredMock> declaredMocks) {
        this.declaredMocks = declaredMocks;
    }


    @RuntimeType
    public Object intercept(@AllArguments Object[] methodArguments,
                            @This Object thisInstance,
                            @Origin Method invokedMethod
    ) throws InvocationTargetException, IllegalAccessException {
        System.err.println("intercepted by mock handler: " + invokedMethod.getName());
        return invokedMethod.invoke(thisInstance, methodArguments);
    }

//    @RuntimeType
//    public Object intercept(@SuperCall Callable<?> zuper) throws Exception {
//        System.err.println("zuper was invoked: ");
//        if (1 < 2) {
//            throw new RuntimeException("the hell");
//        }
//        return zuper.call();
//    }

    public void addDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.addAll(declaredMocksForField);
    }

    public void setDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.clear();
        declaredMocks.addAll(declaredMocksForField);
    }
}
