package io.unlogged.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class StoredCandidate implements Comparable<StoredCandidate> {

    @JsonIgnore
    private long entryProbeIndex;
    private List<Integer> lineNumbers = new ArrayList<>();
    private AtomicAssertion testAssertions = null;
    private String candidateId;
    private String name;
    private String description;
    private List<String> mockIds;
    private List<String> methodArguments;
    private String returnValue;
    private boolean isException;
    private String returnValueClassname;
    private StoredCandidateMetadata metadata;
    private long sessionIdentifier;
    private byte[] probSerializedValue;
    private MethodUnderTest methodUnderTest;
    private StoredCandidate() {
    }

    public List<String> getMockIds() {
        return mockIds;
    }

    public void setMockIds(List<String> mockIds) {
        this.mockIds = mockIds;
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    public void setLineNumbers(List<Integer> lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    @Override
    public int hashCode() {
        if (this.candidateId != null) {
            return this.candidateId.hashCode();
        }
        return (this.sessionIdentifier + "-" + this.metadata.getHostMachineName()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoredCandidate)) {
            return false;
        }
        StoredCandidate otherCandidate = (StoredCandidate) obj;
        if (otherCandidate.getCandidateId() != null && this.getCandidateId() == null) {
            return false;
        }
        if (this.getCandidateId() != null && otherCandidate.getCandidateId() == null) {
            return false;
        }

        if (this.getCandidateId() != null && otherCandidate.getCandidateId() != null) {
            return this.getCandidateId().equals(otherCandidate.getCandidateId());
        }

        return this.sessionIdentifier == otherCandidate.getSessionIdentifier();
    }

    @Override
    public int compareTo(StoredCandidate o) {
        return Long.compare(this.metadata.getTimestamp(), o.metadata.getTimestamp());
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getMethodArguments() {
        return methodArguments;
    }

    public void setMethodArguments(List<String> methodArguments) {
        this.methodArguments = methodArguments;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public boolean isException() {
        return isException;
    }

    public void setException(boolean exception) {
        isException = exception;
    }

    public String getReturnValueClassname() {
        return returnValueClassname;
    }

    public void setReturnValueClassname(String returnValueClassname) {
        this.returnValueClassname = returnValueClassname;
    }

    public StoredCandidateMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(StoredCandidateMetadata metadata) {
        this.metadata = metadata;
    }

    public MethodUnderTest getMethod() {
        return methodUnderTest;
    }

    public void setMethod(MethodUnderTest methodUnderTest1) {
        this.methodUnderTest = methodUnderTest1;
    }

    public long getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void setSessionIdentifier(long sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    @JsonIgnore
    public boolean isReturnValueIsBoolean() {
        return returnValueClassname != null && (returnValueClassname.equals("Z") || returnValueClassname.equals(
                "java.lang.Boolean"));
    }

    public byte[] getProbSerializedValue() {
        return probSerializedValue;
    }

    public void setProbSerializedValue(byte[] probSerializedValue) {
        this.probSerializedValue = probSerializedValue;
    }

    @Override
    public String toString() {
        return "StoredCandidate{" +
                "candidateId='" + candidateId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", methodArguments=" + methodArguments +
                ", returnValue='" + returnValue + '\'' +
                ", isException=" + isException +
                ", returnValueClassname='" + returnValueClassname + '\'' +
                ", metadata=" + metadata +
                ", method=" + methodUnderTest +
                ", entryProbeIndex=" + sessionIdentifier +
                '}';
    }


    public AtomicAssertion getTestAssertions() {
        return testAssertions;
    }

    public void setTestAssertions(AtomicAssertion testAssertions) {
        this.testAssertions = testAssertions;
    }
}
