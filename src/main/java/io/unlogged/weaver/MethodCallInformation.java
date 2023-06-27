package io.unlogged.weaver;

public class MethodCallInformation {
    String subject;
    String methodName;
    boolean isStatic;

    public MethodCallInformation(String subject, String methodName) {
        this.subject = subject;
        this.methodName = methodName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }
}
