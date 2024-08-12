package io.unlogged.autoexecutor.testutils.entity;

import java.util.List;
import java.util.stream.Collectors;

public class TestResultSummary {
    private int numberOfCases;
    private int passingCasesCount;
    private int failingCasesCount;
    private String mode;
    private List<AssertionDetails> failingCases;

    public TestResultSummary(int numberOfCases, int passingCasesCount, int failingCasesCount,
                             List<AssertionDetails> failingCaseIds) {
        this.numberOfCases = numberOfCases;
        this.passingCasesCount = passingCasesCount;
        this.failingCasesCount = failingCasesCount;
        this.failingCases = failingCaseIds;
    }

    public int getNumberOfCases() {
        return numberOfCases;
    }

    public int getPassingCasesCount() {
        return passingCasesCount;
    }

    public int getFailingCasesCount() {
        return failingCasesCount;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<AssertionDetails> getFailingCases() {
        return failingCases;
    }

    public List<Integer> getFailingCaseNumbers() {
        return failingCases.stream().map(AssertionDetails::getCaseId).collect(Collectors.toList());
    }
}
