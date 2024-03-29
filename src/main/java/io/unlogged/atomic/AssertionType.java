package io.unlogged.atomic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public enum AssertionType {
    ALLOF,
    ANYOF,
    NOTALLOF,
    NOTANYOF,
    EQUAL,
    EQUAL_IGNORE_CASE,
    NOT_EQUAL,
    FALSE,
    MATCHES_REGEX,
    NOT_MATCHES_REGEX,
    TRUE,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    NOT_NULL,
    NULL,
    EMPTY,
    NOT_EMPTY,
    CONTAINS_KEY,
    CONTAINS_ITEM,
    NOT_CONTAINS_ITEM,
    CONTAINS_STRING,
    NOT_CONTAINS_KEY,
    STARTS_WITH,
    ENDS_WITH,
    NOT_STARTS_WITH,
    NOT_ENDS_WITH,
    NOT_CONTAINS_STRING;

    private static final Logger logger = LoggerFactory.getLogger(AssertionType.class);

    public static boolean arrayNodeContains(ArrayNode arrayNode, JsonNode node) {
        Stream<JsonNode> nodeStream = StreamSupport.stream(arrayNode.spliterator(), false);
        return nodeStream.anyMatch(j -> j.equals(node));
    }

    @Override
    public String toString() {
        switch (this) {

            case ANYOF:
                return "any of";
            case ALLOF:
                return "all of";
            case NOTALLOF:
                return "not all of";
            case NOTANYOF:
                return "not any of";
            case EQUAL:
                return "equals";
            case EQUAL_IGNORE_CASE:
                return "equals case insensitive";
            case NOT_EQUAL:
                return "not equals";
            case FALSE:
                return "false";
            case TRUE:
                return "true";
            case LESS_THAN:
                return "less than";
            case LESS_THAN_OR_EQUAL:
                return "less than or equal";
            case GREATER_THAN:
                return "greater than";
            case GREATER_THAN_OR_EQUAL:
                return "greater than or equal";
            case MATCHES_REGEX:
                return "matches regex";
            case NOT_MATCHES_REGEX:
                return "not matches regex";
            case NOT_NULL:
                return "is not null";
            case NULL:
                return "is null";
            case EMPTY:
                return "is empty array";
            case NOT_EMPTY:
                return "is not empty array";
            case CONTAINS_KEY:
                return "object has field";
            case CONTAINS_ITEM:
                return "array has item";
            case NOT_CONTAINS_ITEM:
                return "array does not have item";
            case CONTAINS_STRING:
                return "has substring";
            case NOT_CONTAINS_KEY:
                return "object does not have field";
            case NOT_CONTAINS_STRING:
                return "not has substring";
            case STARTS_WITH:
                return "starts with";
            case NOT_STARTS_WITH:
                return "not starts with";
            case ENDS_WITH:
                return "ends with";
            case NOT_ENDS_WITH:
                return "not ends with";
        }
        return "unknown-assertion-type";
    }

    public boolean verify(JsonNode actualValue, JsonNode expectedValue) {

        try {
            String expectedValueString = expectedValue instanceof TextNode ?
                    expectedValue.textValue() : expectedValue.toString();
            String actualValueString = actualValue instanceof TextNode ? actualValue.textValue() :
                    actualValue.toString();
            switch (this) {
                case EQUAL:
                    if (actualValue.isBoolean()) {
                        if (actualValue.booleanValue()) {
                            if (expectedValueString.equals("1") || expectedValueString.equals("true")) {
                                return true;
                            }
                            if (expectedValueString.equals("0") || expectedValueString.equals("false")) {
                                return false;
                            }
                        } else {
                            if (expectedValueString.equals("1") || expectedValueString.equals("true")) {
                                return false;
                            }
                            if (expectedValueString.equals("0") || expectedValueString.equals("false")) {
                                return true;
                            }
                        }
                    }
                    if (expectedValue.isBoolean()) {

                        if (expectedValue.booleanValue()) {
                            if (actualValueString.equals("1") || actualValueString.equals("true")) {
                                return true;
                            }
                            if (actualValueString.equals("0") || actualValueString.equals("false")) {
                                return false;
                            }
                        } else {
                            if (actualValueString.equals("1") || actualValueString.equals("true")) {
                                return false;
                            }
                            if (actualValueString.equals("0") || actualValueString.equals("false")) {
                                return true;
                            }
                        }
                    }
                    if (actualValue instanceof TextNode || expectedValue instanceof TextNode) {
                        return Objects.equals(actualValueString, expectedValueString);
                    }
                    return Objects.equals(actualValue, expectedValue);
                case EQUAL_IGNORE_CASE:
                    return Objects.equals(actualValueString.toLowerCase(), expectedValueString.toLowerCase());
                case NOT_EQUAL:
                    if (actualValue.isBoolean()) {
                        if (actualValue.booleanValue()) {
                            if (expectedValueString.equals("1") || expectedValueString.equals("true")) {
                                return false;
                            }
                            if (expectedValueString.equals("0") || expectedValueString.equals("false")) {
                                return true;
                            }
                        } else {
                            if (expectedValueString.equals("1") || expectedValueString.equals("true")) {
                                return true;
                            }
                            if (expectedValueString.equals("0") || expectedValueString.equals("false")) {
                                return false;
                            }
                        }
                    }
                    if (expectedValue.isBoolean()) {

                        if (expectedValue.booleanValue()) {
                            if (actualValueString.equals("1") || actualValueString.equals("true")) {
                                return false;
                            }
                            if (actualValueString.equals("0") || actualValueString.equals("false")) {
                                return true;
                            }
                        } else {
                            if (actualValueString.equals("1") || actualValueString.equals("true")) {
                                return true;
                            }
                            if (actualValueString.equals("0") || actualValueString.equals("false")) {
                                return false;
                            }
                        }
                    }
                    if (actualValue instanceof TextNode || expectedValue instanceof TextNode) {
                        return !Objects.equals(actualValueString, expectedValueString);
                    }
                    return !Objects.equals(actualValue, expectedValue);
                case FALSE:
                    return Objects.equals(actualValue.asBoolean(), false);
                case TRUE:
                    return Objects.equals(actualValue.asBoolean(), true);
                case LESS_THAN:
                    return actualValue.asDouble() < expectedValue.asDouble();
                case GREATER_THAN:
                    return actualValue.asDouble() > expectedValue.asDouble();
                case LESS_THAN_OR_EQUAL:
                    return actualValue.asDouble() <= expectedValue.asDouble();
                case GREATER_THAN_OR_EQUAL:
                    return actualValue.asDouble() >= expectedValue.asDouble();
                case NULL:
                    return actualValue.isNull();
                case EMPTY:
                    return actualValue.isEmpty();
                case NOT_EMPTY:
                    return !actualValue.isEmpty();
                case NOT_NULL:
                    return !actualValue.isNull();
                case MATCHES_REGEX:
                    return Pattern.compile(expectedValueString).matcher(actualValueString).matches();
                case NOT_MATCHES_REGEX:
                    return !Pattern.compile(expectedValueString).matcher(actualValueString).matches();
                case CONTAINS_KEY:
                    return actualValue.has(expectedValueString);
                case NOT_CONTAINS_KEY:
                    return !actualValue.has(expectedValueString);
                case CONTAINS_ITEM:
                    return arrayNodeContains((ArrayNode) actualValue, expectedValue);
                case NOT_CONTAINS_ITEM:
                    return !arrayNodeContains((ArrayNode) actualValue, expectedValue);
                case CONTAINS_STRING:
                    return actualValueString.contains(expectedValueString);
                case NOT_CONTAINS_STRING:
                    return !actualValueString.contains(expectedValueString);

                case ENDS_WITH:
                    return actualValueString.endsWith(expectedValueString);

                case NOT_ENDS_WITH:
                    return !actualValueString.endsWith(expectedValueString);


                case STARTS_WITH:
                    return actualValueString.startsWith(expectedValueString);

                case NOT_STARTS_WITH:
                    return !actualValueString.startsWith(expectedValueString);

            }
            return false;
        } catch (Exception e) {
            logger.warn("Assertion exception: ", e);
            return false;
        }
    }
}
