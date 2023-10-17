package io.unlogged.atomic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AtomicAssertion {

    List<AtomicAssertion> subAssertions = new ArrayList<>();
    private Expression expression = Expression.SELF;
    private String expectedValue;
    private String id = UUID.randomUUID().toString();
    private AssertionType assertionType = AssertionType.EQUAL;
    private String key;

    public AtomicAssertion() {
    }

    public AtomicAssertion(AtomicAssertion original) {
        if (original == null) {
            return;
        }
        this.expression = original.expression;
        this.assertionType = original.assertionType;
        this.key = original.key;
        this.id = original.id;
        this.expectedValue = original.expectedValue;
        this.subAssertions = original.subAssertions.stream()
                .map(AtomicAssertion::new).collect(Collectors.toList());
    }

    public AtomicAssertion(Expression expression, AssertionType assertionType, String key, String expectedValue) {
        this.expression = expression;
        this.assertionType = assertionType;
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public AtomicAssertion(AssertionType assertionType, String key, String expectedValue) {
        this.expression = Expression.SELF;
        this.assertionType = assertionType;
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public AtomicAssertion(AssertionType assertionType, List<AtomicAssertion> subAssertions) {
        this.expression = Expression.SELF;
        if (assertionType != AssertionType.ANYOF && assertionType != AssertionType.ALLOF) {
            // unacceptable
        }
        this.assertionType = assertionType;
        this.subAssertions = subAssertions;
    }

    public List<AtomicAssertion> getSubAssertions() {
        return subAssertions;
    }

    public void setSubAssertions(List<AtomicAssertion> subAssertions) {
        this.subAssertions = subAssertions;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AssertionType getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(AssertionType assertionType) {
        this.assertionType = assertionType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "AtomicAssertion{ " +
                expression +
                " (" + key + ")" +
                " " + assertionType +
                " = " + expectedValue +
                " }";
    }
}
