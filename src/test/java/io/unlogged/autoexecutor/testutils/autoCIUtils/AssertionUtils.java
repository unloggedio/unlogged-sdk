package io.unlogged.autoexecutor.testutils.autoCIUtils;


import io.unlogged.autoexecutor.testutils.entity.AutoAssertionResult;
import io.unlogged.autoexecutor.testutils.entity.DifferenceResult;
import io.unlogged.autoexecutor.testutils.entity.TestUnit;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;

import java.util.HashMap;
import java.util.Map;

import static io.unlogged.autoexecutor.testutils.entity.DiffResultType.SAME;

public class AssertionUtils {

    public static AutoAssertionResult assertCase(TestUnit testUnit) {
        AutoAssertionResult assertionResult = new AutoAssertionResult();
        String assertionType = testUnit.getAssertionType();
        String refOut = testUnit.getReferenceValue();

        AgentCommandResponse agentCommandResponse = postProcessResponse(testUnit.getResponse());
        ResponseType actualResponseType = agentCommandResponse.getResponseType();
        String actualResponse = String.valueOf(agentCommandResponse.getMethodReturnValue());
        if (actualResponse == null) {
            actualResponse = "null - from agent";
        }

        assertionResult.setExpected(refOut);
        assertionResult.setActual(actualResponse);

        boolean result = false;
        assertionResult.setAssertionType(assertionType);
        String message = "";
        switch (assertionType) {
            case "EQUAL":
                //absolute equal check
                if (refOut.equals(actualResponse)) {
                    result = true;
                    message = "Responses are Equal";
                } else {
                    message = "Responses are Not Equal";
                }
                break;
            case "SIMILAR":
                //similar regex check
                Map<String, String> diffResNormal = areTextsSimilar(refOut, actualResponse);
                if (diffResNormal.get("status").equals("true")) {
                    result = true;
                }
                message = diffResNormal.get("reason");
                break;
            case "SIMILAR EXCEPTION":
                //should be a similar exception
                Map<String, String> diffResException = areTextsSimilar(refOut, actualResponse);
                if (actualResponseType.equals(ResponseType.EXCEPTION)) {
                    if (diffResException.get("status").equals("true")) {
                        result = true;
                    }
                    message = diffResException.get("reason");
                } else {
                    message = "Response is not an Exception, Exception expected";
                }
                break;
            case "NO EXCEPTION":
                //should not throw an exception
                if (!actualResponseType.equals(ResponseType.EXCEPTION)) {
                    message = "Did not throw an Exception, as expected";
                    result = true;
                } else {
                    message = "Received Exception when it was not expected";
                }
                assertionResult.setExpected("NO EXCEPTION");
                assertionResult.setActual(actualResponse);
                break;
            case "EQUAL EXCEPTION":
                //should be an equal exception
                if (actualResponseType.equals(ResponseType.EXCEPTION)
                        && actualResponse.equals(refOut)) {
                    result = true;
                    message = "Received Exception is the same as the Excepted Exception";
                } else {
                    message = "Received Exception is different from expected Exception";
                }
                break;
            case "NOT NULL":
                //the response shouldn't be null
                if (!actualResponse.equals("null - from agent")) {
                    result = true;
                    message = "Received a non-null response";
                } else {
                    message = "Received a null response";
                }
                assertionResult.setExpected("NOT NULL");
                assertionResult.setActual(actualResponse);
                break;
            case "NOT EQUAL":
                //the response shouldn't be equal to reference output
                if (!actualResponse.equals(refOut)) {
                    result = true;
                    message = "Response is not equal to reference, as expected";
                } else {
                    message = "Response is equal to reference, this was not expected";
                }
        }
        assertionResult.setPassing(result);
        assertionResult.setMessage(message);

        return assertionResult;
    }

    private static AgentCommandResponse postProcessResponse(AgentCommandResponse response) {
        ResponseType responseType = response.getResponseType();
        switch (responseType) {
            case FAILED:
                //failed to be marked as exceptions
                response.setResponseType(ResponseType.EXCEPTION);
                break;
            case EXCEPTION:
                //exception case post processing
                break;
            case NORMAL:
                //normal case post processing
                if (response.getMethodReturnValue() == null) {
                    response.setMethodReturnValue("null - from agent");
                }
                String responseValue = String.valueOf(response.getMethodReturnValue());
                if (responseValue.startsWith("Failed to serialize")) {
                    response.setResponseType(ResponseType.EXCEPTION);
                }
                break;
        }
        return response;
    }

    private static Map<String, String> areTextsSimilar(String expected, String actual) {
        Map<String, String> diffInfo = new HashMap<>();
        boolean similar = false;
        DifferenceResult differenceResult = DiffUtils.calculateDifferencesAeCi(expected, actual);
        if (differenceResult.getDiffResultType().equals(SAME)) {
            similar = true;
            diffInfo.put("reason", "Response values are Equal");
        } else {
            //ensure lefts and rights are empty/null - no structural change
            if (differenceResult.getLeftOnly() != null && differenceResult.getRightOnly() != null) {
                if (differenceResult.getLeftOnly().isEmpty() && differenceResult.getRightOnly().isEmpty()) {
                    similar = true;
                    diffInfo.put("reason", "Response structures are similar");
                }
            } else {
                //for primitives, +1 check for null pending
                similar = true;
                diffInfo.put("reason", "Response types are same");
            }
        }
        diffInfo.put("status", String.valueOf(similar));
        if (!similar) {
            diffInfo.put("reason", "Responses are not similar");
        }
        return diffInfo;
    }
}
