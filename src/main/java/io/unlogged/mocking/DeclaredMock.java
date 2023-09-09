package io.unlogged.mocking;

import java.util.List;

public class DeclaredMock {

    private final String fieldTypeName;
    private final String fieldName;
    private final String methodName;
    private final List<ParameterMatcher> whenParameter;
    private final ReturnValue returnParameter;
    private final MethodExitType methodExitType;

    public DeclaredMock(String fieldTypeName, String fieldName, String methodName,
                        List<ParameterMatcher> whenParameter, ReturnValue returnParameter, MethodExitType methodExitType) {
        this.fieldTypeName = fieldTypeName;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.whenParameter = whenParameter;
        this.returnParameter = returnParameter;
        this.methodExitType = methodExitType;
    }

    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ParameterMatcher> getWhenParameter() {
        return whenParameter;
    }

    public ReturnValue getReturnParameter() {
        return returnParameter;
    }

    public MethodExitType getReturnType() {
        return methodExitType;
    }
}
