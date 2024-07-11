package io.unlogged.logging.perthread;

public class MethodMetadata {

    private String className;
    private String methodName;
    private String argumentTypes;
    private String returnType;
    private String methodDesc;
    private boolean isStatic;
    private boolean isPublic;
    private boolean usesFields;
    private int methodAccess;

    public MethodMetadata(
            String className,
            String methodName,
            String argumentTypes,
            String returnType,
            boolean isStatic,
            boolean isPublic,
            boolean usesFields,
            int methodAccess,
            String methodDesc) {

        this.className = className;
        this.methodName = methodName;
        this.argumentTypes = argumentTypes;
        this.returnType = returnType;
        this.isStatic = isStatic;
        this.isPublic = isPublic;
        this.usesFields = usesFields;
        this.methodAccess = methodAccess;
        this.methodDesc = methodDesc;
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

    public String getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(String argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isUsesFields() {
        return usesFields;
    }

    public void setUsesFields(boolean usesFields) {
        this.usesFields = usesFields;
    }

    public int getMethodAccess() {
        return methodAccess;
    }

    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }
}
