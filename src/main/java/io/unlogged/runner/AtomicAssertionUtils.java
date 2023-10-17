package io.unlogged.runner;


import io.unlogged.atomic.AtomicAssertion;

import java.util.ArrayList;
import java.util.List;

public class AtomicAssertionUtils {

    public static List<AtomicAssertion> flattenAssertionMap(AtomicAssertion testAssertions) {

        List<AtomicAssertion> all = new ArrayList<>();
        all.add(testAssertions);

        if (testAssertions.getSubAssertions() != null) {
            for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
                all.addAll(flattenAssertionMap(subAssertion));
            }
        }

        return all;
    }

    public static int countAssertions(AtomicAssertion testAssertions) {

        if (testAssertions.getSubAssertions() == null || testAssertions.getSubAssertions().size() == 0) {
            return 1;
        }

        int count = 0;

        for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
            count += countAssertions(subAssertion);
        }

        return count;
    }


}
