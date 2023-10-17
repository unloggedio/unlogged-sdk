package io.unlogged.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class MethodUnderTest {
    String name;
    String signature;
    String className;
    int methodHash;


    public MethodUnderTest(String name, String signature, int methodHash, String className) {
        this.name = name;
        this.signature = signature;
        this.className = className;
        this.methodHash = methodHash;
    }

    public MethodUnderTest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getMethodHash() {
        return methodHash;
    }

    public void setMethodHash(int methodHash) {
        this.methodHash = methodHash;
    }

    @Override
    public String toString() {
        return "MethodUnderTest{" +
                className + "." + name + "(" + signature + ") " + methodHash +
                '}';
    }

    @JsonIgnore
    public String getMethodHashKey() {
        return className + "#" + name + "#" + signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodUnderTest that = (MethodUnderTest) o;
        return methodHash == that.methodHash
                && Objects.equals(name, that.name)
                && Objects.equals(signature, that.signature)
                && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, signature, className, methodHash);
    }
}
