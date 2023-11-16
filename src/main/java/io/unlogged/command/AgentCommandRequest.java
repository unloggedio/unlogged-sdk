package io.unlogged.command;

import io.unlogged.auth.RequestAuthentication;
import io.unlogged.mocking.DeclaredMock;

import java.util.ArrayList;
import java.util.List;

public class AgentCommandRequest {
    RequestAuthentication requestAuthentication;
    private AgentCommand command;
    private String className;
    private List<String> alternateClassNames;
    private String methodName;
    private String methodSignature;
    private List<String> methodParameters;
    private AgentCommandRequestType requestType;
    private List<String> parameterTypes;
    private List<DeclaredMock> declaredMocks = new ArrayList<>();

    public RequestAuthentication getRequestAuthentication() {
        return requestAuthentication;
    }

    public void setRequestAuthentication(RequestAuthentication requestAuthentication) {
        this.requestAuthentication = requestAuthentication;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<String> getAlternateClassNames() {
        return alternateClassNames;
    }

    public void setAlternateClassNames(List<String> alternateClassNames) {
        this.alternateClassNames = alternateClassNames;
    }

    public AgentCommandRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(AgentCommandRequestType requestType) {
        this.requestType = requestType;
    }

    public AgentCommand getCommand() {
        return command;
    }

    public void setCommand(AgentCommand command) {
        this.command = command;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public List<String> getMethodParameters() {
        return methodParameters;
    }

    public void setMethodParameters(List<String> methodParameters) {
        this.methodParameters = methodParameters;
    }

    @Override
    public String toString() {
        return "AgentCommandRequest{" +
                "command=" + command +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", methodParameters=" + methodParameters +
                '}';
    }

    public List<DeclaredMock> getDeclaredMocks() {
        return declaredMocks;
    }

    public void setDeclaredMocks(List<DeclaredMock> declaredMocks) {
        this.declaredMocks = declaredMocks;
    }

}
