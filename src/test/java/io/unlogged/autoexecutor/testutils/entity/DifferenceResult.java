package io.unlogged.autoexecutor.testutils.entity;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class DifferenceResult {
    private final List<DifferenceInstance> differenceInstanceList;
    private final JsonNode leftOnly;
    private final JsonNode rightOnly;
    private DiffResultType diffResultType;

    public DifferenceResult(List<DifferenceInstance> differenceInstanceList,
                            DiffResultType diffResultType,
                            JsonNode leftOnly,
                            JsonNode rightOnly) {
        this.differenceInstanceList = differenceInstanceList;
        this.diffResultType = diffResultType;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    public JsonNode getLeftOnly() {
        return leftOnly;
    }

    public JsonNode getRightOnly() {
        return rightOnly;
    }

    public DiffResultType getDiffResultType() {
        return diffResultType;
    }

    @Override
    public String toString() {
        return "DifferenceResult{" +
                "differenceInstanceList=" + differenceInstanceList +
                ", diffResultType=" + diffResultType +
                ", leftOnly=" + leftOnly +
                ", rightOnly=" + rightOnly +
                '}';
    }
}
