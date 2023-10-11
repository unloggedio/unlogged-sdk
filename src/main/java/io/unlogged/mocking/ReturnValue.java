package io.unlogged.mocking;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReturnValue {
    private String value;
    private ReturnValueType returnValueType;
    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private String className;

    public ReturnValue(String value, String returnValueClassName, ReturnValueType returnValueType) {
        this.value = value;
        this.className = returnValueClassName;
        this.returnValueType = returnValueType;
    }

    public ReturnValue(String value, String returnValueClassName, ReturnValueType returnValueType, List<DeclaredMock> declaredMocks) {
        this.value = value;
        this.className = returnValueClassName;
        this.returnValueType = returnValueType;
        this.declaredMocks.addAll(declaredMocks);
    }

    public ReturnValue() {
    }

    public ReturnValue(ReturnValue returnParameter) {
        this.value = returnParameter.value;
        this.className = returnParameter.className;
        this.returnValueType = returnParameter.returnValueType;
        this.declaredMocks.addAll(returnParameter.declaredMocks
                .stream().map(DeclaredMock::new).collect(Collectors.toList()));

    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }

    public ReturnValueType getReturnValueType() {
        return returnValueType;
    }

    public void addDeclaredMock(DeclaredMock mockDefinition) {
        declaredMocks.add(mockDefinition);
    }

    public List<DeclaredMock> getDeclaredMocks() {
        return declaredMocks;
    }

    @Override
    public String toString() {
        return "ReturnValue{" +
                "value='" + value + '\'' +
                ", returnValueType=" + returnValueType +
                ", className='" + className + '\'' +
                '}';
    }
}
