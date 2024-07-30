package io.unlogged.autoexecutor.testutils.entity;

public class AutoAssertionResult {
    private boolean passing;
    private String message;
    private String assertionType;

    public boolean isPassing() {
        return passing;
    }

    public void setPassing(boolean passing) {
        this.passing = passing;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(String assertionType) {
        this.assertionType = assertionType;
    }
}
