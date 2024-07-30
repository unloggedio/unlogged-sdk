package io.unlogged.autoexecutor.testutils.entity;

import java.util.List;

public class TestResultSummary {
    private int numberOfCases;
    private int passingCasesCount;
    private int failingCasesCount;
    private String mode;
    private List<Integer> failingCases;

    public TestResultSummary(int numberOfCases, int passingCasesCount, int failingCasesCount,
                             List<Integer> failingCaseIds) {
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

    public List<Integer> getFailingCases() {
        return failingCases;
    }
}
