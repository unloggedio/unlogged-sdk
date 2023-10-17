package io.unlogged.runner;

public class ClassUnderTest {
    private final String qualifiedClassName;

    public ClassUnderTest(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    public String getQualifiedClassName() {
        return qualifiedClassName;
    }
}
