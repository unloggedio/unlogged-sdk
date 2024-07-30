package io.unlogged.autoexecutor.testutils.entity;

public class ValueDifference {
    private final String leftValue;
    private final String rightValue;

    public String leftValue() {
        return leftValue;
    }

    public String rightValue() {
        return rightValue;
    }

    public ValueDifference(String leftValue, String rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }
}
