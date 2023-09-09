package io.unlogged.mocking;

import java.util.ArrayList;
import java.util.List;

public class ReturnValue {
    private final String value;
    private final ReturnValueType returnValueType;
    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private final String className;

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
}
