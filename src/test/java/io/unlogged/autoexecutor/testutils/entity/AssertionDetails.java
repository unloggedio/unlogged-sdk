package io.unlogged.autoexecutor.testutils.entity;

public class AssertionDetails {
    private int caseId;
    private String expected;
    private String actual;
    private String assertionType;

    public AssertionDetails(int caseId, String expected, String actual, String assertionType) {
        this.caseId = caseId;
        this.expected = expected;
        this.actual = actual;
        this.assertionType = assertionType;
    }

    public int getCaseId() {
        return caseId;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }

    public String getAssertionType() {
        return assertionType;
    }
}
