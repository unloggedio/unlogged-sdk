package io.unlogged.runner;

import io.unlogged.AgentCommandRawResponse;
import io.unlogged.atomic.AssertionResult;

public class AssertionResultWithRawObject {
    private final AssertionResult assertionResult;
    private final AgentCommandRawResponse responseObject;

    public AssertionResultWithRawObject(AssertionResult assertionResult, AgentCommandRawResponse responseObject) {
        this.assertionResult = assertionResult;
        this.responseObject = responseObject;
    }

    public AssertionResult getAssertionResult() {
        return assertionResult;
    }

    public AgentCommandRawResponse getResponseObject() {
        return responseObject;
    }
}
