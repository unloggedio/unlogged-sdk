package io.unlogged.atomic;

import java.util.HashMap;
import java.util.Map;

public class AssertionResult {
    private final Map<String, Boolean> results = new HashMap<>();
    private boolean passing = false;

    public void addResult(AtomicAssertion assertion, boolean result) {
        this.results.put(assertion.getId(), result);
    }

    public Map<String, Boolean> getResults() {
        return results;
    }

    public boolean isPassing() {
        return passing;
    }

    public void setPassing(boolean finalResult) {
        this.passing = finalResult;
    }
}
