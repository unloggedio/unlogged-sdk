package io.unlogged.runner;


import io.unlogged.atomic.StoredCandidate;
import io.unlogged.mocking.DeclaredMock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtomicRecord {

    private String classname;
    private Map<String, List<StoredCandidate>> storedCandidateMap = new HashMap<>();
    private Map<String, List<DeclaredMock>> declaredMockMap = new HashMap<>();

    public AtomicRecord(String classname) {
        this.classname = classname;
    }

    public AtomicRecord() {
    }

    public Map<String, List<DeclaredMock>> getDeclaredMockMap() {
        return declaredMockMap;
    }

    public void setDeclaredMockMap(Map<String, List<DeclaredMock>> declaredMockMap) {
        this.declaredMockMap = declaredMockMap;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public Map<String, List<StoredCandidate>> getStoredCandidateMap() {
        return storedCandidateMap;
    }

    public void setStoredCandidateMap(Map<String, List<StoredCandidate>> storedCandidateList) {
        this.storedCandidateMap = storedCandidateList;
    }

    @Override
    public String toString() {
        return "AtomicRecord{" +
                "classname='" + classname + '\'' +
                ", storedCandidateList=" + storedCandidateMap.size() + " candidates" +
                ", declaredMocks=" + declaredMockMap.size() + " mocks" +
                '}';
    }
}
