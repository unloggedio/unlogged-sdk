package io.unlogged.command;

public class AgentCommandResponse {
    private Object methodReturnValue;
    private ResponseType responseType;
    private String responseClassName;
    private String message;
    private String targetMethodName;
    private String targetClassName;
    private String targetMethodSignature;
    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public String getTargetMethodName() {
        return targetMethodName;
    }

    public void setTargetMethodName(String targetMethodName) {
        this.targetMethodName = targetMethodName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public String getTargetMethodSignature() {
        return targetMethodSignature;
    }

    public void setTargetMethodSignature(String targetMethodSignature) {
        this.targetMethodSignature = targetMethodSignature;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public String getResponseClassName() {
        return responseClassName;
    }

    public void setResponseClassName(String responseClassName) {
        this.responseClassName = responseClassName;
    }

    public Object getMethodReturnValue() {
        return methodReturnValue;
    }

    public void setMethodReturnValue(Object methodReturnValue) {
        this.methodReturnValue = methodReturnValue;
    }

    @Override
    public String toString() {
        return "AgentCommandResponse{" +
                "methodReturnValue=" + methodReturnValue +
                ", message='" + message + '\'' +
                '}';
    }

    public void setTimestamp(long time) {
        this.timestamp = time;
    }
}
