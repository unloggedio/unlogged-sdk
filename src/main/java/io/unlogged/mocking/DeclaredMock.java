package io.unlogged.mocking;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeclaredMock {

    private String id;
    private String name;
    private String fieldTypeName;
    private String fieldName;
    private String sourceClassName;
    private String methodName;
    private List<ParameterMatcher> whenParameter;
    private List<ThenParameter> thenParameter;
    public DeclaredMock() {
		this.methodName = "default value";
    }

    public DeclaredMock(DeclaredMock declaredMock) {
        this.name = declaredMock.name;
        this.id = declaredMock.id;
        this.fieldTypeName = declaredMock.fieldTypeName;
        this.sourceClassName = declaredMock.sourceClassName;
        this.fieldName = declaredMock.fieldName;
        this.whenParameter = declaredMock.whenParameter.stream()
                .map(ParameterMatcher::new).collect(Collectors.toList());
        this.thenParameter = declaredMock.thenParameter
                .stream().map(ThenParameter::new).collect(Collectors.toList());

		if (declaredMock.methodName == null) {
			this.methodName = "default value";
		}
		else {
			this.methodName = declaredMock.methodName;
		}
    }

    public DeclaredMock(String name, String fieldTypeName, String fieldName, String methodName,
                        List<ParameterMatcher> whenParameterLists,
                        List<ThenParameter> thenParameterList
    ) {
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.whenParameter = whenParameterLists;
        this.thenParameter = thenParameterList;
    }

    public String getSourceClassName() {
        return sourceClassName;
    }

    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public void setFieldTypeName(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<ParameterMatcher> getWhenParameter() {
        return whenParameter;
    }

    public void setWhenParameter(List<ParameterMatcher> whenParameter) {
        this.whenParameter = whenParameter;
    }

    public List<ThenParameter> getThenParameter() {
        return thenParameter;
    }

    public void setThenParameter(List<ThenParameter> thenParameter) {
        this.thenParameter = thenParameter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeclaredMock that = (DeclaredMock) o;
        return Objects.equals(name, that.name) && fieldTypeName.equals(that.fieldTypeName) && fieldName.equals(
                that.fieldName) && methodName.equals(that.methodName) && whenParameter.equals(that.whenParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fieldTypeName, fieldName, methodName, whenParameter);
    }

    @Override
    public String toString() {
        return "DeclaredMock{" +
                "name='" + name + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", sourceClassName='" + sourceClassName + '\'' +
                '}';
    }
}
