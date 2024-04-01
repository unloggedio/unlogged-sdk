package io.unlogged.atomic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.logging.ObjectMapperFactory;

import java.util.List;
import java.util.Objects;

public class AssertionEngine {

    private static final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    public static AssertionResult executeAssertions(
            AtomicAssertion assertion, JsonNode responseNode
    ) {
        AssertionResult assertionResult = new AssertionResult();
        if (assertion == null) {
            assertionResult.setPassing(true);
            return assertionResult;
        }

        AssertionType assertionType = assertion.getAssertionType();

        boolean result;
        if (assertionType == AssertionType.ANYOF) {
            result = false;
            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = result || subResult.isPassing();
            }

        } else if (assertionType == AssertionType.ALLOF) {

            result = true;
            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = result && subResult.isPassing();
            }

        } else if (assertionType == AssertionType.NOTALLOF) {

            result = true;

            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = !subResult.isPassing();
            }

        } else if (assertionType == AssertionType.NOTANYOF) {

            result = false;

            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = result || !subResult.isPassing();
            }

        } else {
            String key = assertion.getKey();
            if (Objects.equals(key, "/")) {
                key = "";
            }
            JsonNode assertionActualValue = responseNode == null ? null : responseNode.at(key);
            Expression expression = assertion.getExpression();
            JsonNode expressedValue;
            if (assertionActualValue != null) {
                expressedValue = expression.compute(assertionActualValue);
            } else {
                expressedValue = assertionActualValue;
            }
            JsonNode expectedValue = null;
            try {
                if (assertion.getExpectedValue() == null) {
                    result = assertionType.verify(expressedValue, null);
                } else {
                    expectedValue = objectMapper.readTree(assertion.getExpectedValue());
                    result = assertionType.verify(expressedValue, expectedValue);
                }
            } catch (JsonProcessingException e) {
                try {
                    expectedValue = objectMapper.readTree("\"" + assertion.getExpectedValue() + "\"");
                    result = assertionType.verify(expressedValue, expectedValue);
                } catch (JsonProcessingException ex) {
                    result = false;
                }
//                throw new RuntimeException(e);
            }
        }


        assertionResult.addResult(assertion, result);
        assertionResult.setPassing(result);

        return assertionResult;
    }

}
