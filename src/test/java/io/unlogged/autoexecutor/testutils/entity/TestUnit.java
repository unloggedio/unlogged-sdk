package io.unlogged.autoexecutor.testutils.entity;


import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandResponse;

public class TestUnit {
    private String classname;
    private String methodName;
    private String methodSign;
    private String input;
    private String assertionType;
    private String referenceValue;
    private String refResponseType;
    private AgentCommandRequest sentRequest;
    private AgentCommandResponse response;

    public TestUnit(String classname, String methodName, String methodSign, String input, String assertionType, String referenceValue, String refResponseType, AgentCommandRequest sentRequest, AgentCommandResponse response) {
        this.classname = classname;
        this.methodName = methodName;
        this.methodSign = methodSign;
        this.input = input;
        this.assertionType = assertionType;
        this.referenceValue = referenceValue;
        this.sentRequest = sentRequest;
        this.response = response;
        this.refResponseType = refResponseType;
    }

    public String getAssertionType() {
        return assertionType;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public AgentCommandResponse getResponse() {
        return response;
    }
}
