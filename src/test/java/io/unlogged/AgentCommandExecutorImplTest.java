package io.unlogged;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.logging.DiscardEventLogger;
import io.unlogged.mocking.*;
import io.unlogged.test.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AgentCommandExecutorImplTest {

    @Test
    void arrangeMocks() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        AgentCommandExecutorImpl agentCommandExecutor = new AgentCommandExecutorImpl(objectMapper,
                new DiscardEventLogger());

        SimpleServiceA simpleServiceA = new SimpleServiceA(new SimpleServiceB());


        List<DeclaredMock> declaredMocks = new ArrayList<>();
        declaredMocks.add(new DeclaredMock("mock name",
                SimplePojoB.class.getCanonicalName(), "simpleServiceB",
                "makeAndReturn",
                Arrays.asList(new ParameterMatcher("fieldName", ParameterMatcherType.ANY,
                        SimplePojoB.class.getCanonicalName())),
                Arrays.asList(new ThenParameter(
                        new ReturnValue("{\"aStringValue\": \"mockedStringResponse\", \"aLongValue\": 9988}",
                                SimplePojoA.class.getCanonicalName(), ReturnValueType.REAL),
                        MethodExitType.NORMAL))));

        SimplePojoA expectedValue = simpleServiceA.callToTest("a val", new SimplePojoB("1", 1));
//        System.out.println("Expected value: " + objectMapper.writeValueAsString(expectedValue));

        SimpleServiceA simpleServiceAWithMocks = (SimpleServiceA) agentCommandExecutor.arrangeMocks(
                simpleServiceA.getClass(),
                simpleServiceA.getClass().getClassLoader(),
                simpleServiceA, declaredMocks);


        SimplePojoA returnedValue = simpleServiceAWithMocks.callToTest("a val", new SimplePojoB("1", 1));
        String mockedResponseString = objectMapper.writeValueAsString(returnedValue);
//        System.out.println("Mocked value: " + mockedResponseString);
        Assertions.assertTrue(mockedResponseString.contains("9988"));
        Assertions.assertTrue(mockedResponseString.contains("mockedStringResponse"));


        SimplePojoA originalValueAgain = simpleServiceAWithMocks.callToTest1("a val", new SimplePojoB("1", 1));
        String originalResponseString = objectMapper.writeValueAsString(originalValueAgain);
        Assertions.assertTrue(originalResponseString.contains("a val-991"));
        Assertions.assertTrue(originalResponseString.contains("7"));
//        System.out.println("Original value: " + originalResponseString);
    }

    @Test
    void arrangeMocks1() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        AgentCommandExecutorImpl agentCommandExecutor = new AgentCommandExecutorImpl(objectMapper,
                new DiscardEventLogger());

        SimpleServiceA simpleServiceA = new ExtendedSimpleServiceA(new SimpleServiceB());


        List<DeclaredMock> declaredMocks = new ArrayList<>();
        declaredMocks.add(new DeclaredMock("mock name", SimplePojoB.class.getCanonicalName(), "simpleServiceB",
                "makeAndReturn",
                Arrays.asList(new ParameterMatcher("fieldname", ParameterMatcherType.ANY,
                        SimplePojoB.class.getCanonicalName())),
                Arrays.asList(new ThenParameter(
                        new ReturnValue("{\"aStringValue\": \"mockedStringResponse\", \"aLongValue\": 9988}",
                                SimplePojoA.class.getCanonicalName(), ReturnValueType.REAL),
                        MethodExitType.NORMAL)
                )));

        SimplePojoA expectedValue = simpleServiceA.callToTest("a val", new SimplePojoB("1", 1));
//        System.out.println("Expected value: " + objectMapper.writeValueAsString(expectedValue));

        SimpleServiceA simpleServiceAWithMocks = (SimpleServiceA) agentCommandExecutor.arrangeMocks(
                simpleServiceA.getClass(),
                simpleServiceA.getClass().getClassLoader(),
                simpleServiceA, declaredMocks);


        SimplePojoA returnedValue = simpleServiceAWithMocks.callToTest("a val", new SimplePojoB("1", 1));
        String mockedResponseString = objectMapper.writeValueAsString(returnedValue);
        Assertions.assertTrue(mockedResponseString.contains("9988"));
        Assertions.assertTrue(mockedResponseString.contains("mockedStringResponse"));
//        System.out.println("Mocked value: " + mockedResponseString);
        SimplePojoA originalValueAgain = simpleServiceAWithMocks.callToTest1("a val", new SimplePojoB("1", 1));
        String originalResponseString = objectMapper.writeValueAsString(originalValueAgain);
        Assertions.assertTrue(originalResponseString.contains("a val-991"));
        Assertions.assertTrue(originalResponseString.contains("7"));
//        System.out.println("Original value: " + originalResponseString);
    }

    @Test
    void arrangeMockThrows() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        AgentCommandExecutorImpl agentCommandExecutor = new AgentCommandExecutorImpl(objectMapper,
                new DiscardEventLogger());

        SimpleServiceA simpleServiceA = new ExtendedSimpleServiceA(new SimpleServiceB());


        List<DeclaredMock> declaredMocks = new ArrayList<>();
        declaredMocks.add(new DeclaredMock("mockName", SimplePojoB.class.getCanonicalName(), "simpleServiceB",
                "makeAndReturn",
                Arrays.asList(new ParameterMatcher("fieldName", ParameterMatcherType.ANY,
                        SimplePojoB.class.getCanonicalName())),
                Arrays.asList(new ThenParameter(new ReturnValue("{\"message\": \"exception message\"}",
                        WhatException.class.getCanonicalName(), ReturnValueType.REAL),
                        MethodExitType.EXCEPTION))));

        SimplePojoA expectedValue = simpleServiceA.callToTest("a val", new SimplePojoB("1", 1));
//        System.out.println("Expected value: " + objectMapper.writeValueAsString(expectedValue));

        SimpleServiceA simpleServiceAWithMocks = (SimpleServiceA) agentCommandExecutor.arrangeMocks(
                simpleServiceA.getClass(),
                simpleServiceA.getClass().getClassLoader(),
                simpleServiceA, declaredMocks);


        try {
            SimplePojoA returnedValue = simpleServiceAWithMocks.callToTest("a val", new SimplePojoB("1", 1));
            Assertions.fail();
        } catch (Exception e) {
//            System.out.println("Exception class: " + e.getClass().getCanonicalName());
            Assertions.assertTrue(e instanceof WhatException);
            Assertions.assertEquals(null, e.getMessage());
        }
//        System.out.println("Mocked value: " + mockedResponseString);
        SimplePojoA originalValueAgain = simpleServiceAWithMocks.callToTest1("a val", new SimplePojoB("1", 1));
        String originalResponseString = objectMapper.writeValueAsString(originalValueAgain);
        Assertions.assertTrue(originalResponseString.contains("a val-991"));
        Assertions.assertTrue(originalResponseString.contains("7"));
//        System.out.println("Original value: " + originalResponseString);
    }

    @Test
    void arrangeMockThrowsWithMessage() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        AgentCommandExecutorImpl agentCommandExecutor = new AgentCommandExecutorImpl(objectMapper,
                new DiscardEventLogger());

        SimpleServiceA simpleServiceA = new ExtendedSimpleServiceA(new SimpleServiceB());


        List<DeclaredMock> declaredMocks = new ArrayList<>();
        declaredMocks.add(new DeclaredMock("mockName", SimplePojoB.class.getCanonicalName(), "simpleServiceB",
                "makeAndReturn",
                Arrays.asList(new ParameterMatcher("fieldName", ParameterMatcherType.ANY,
                        SimplePojoB.class.getCanonicalName())),
                Arrays.asList(new ThenParameter(
                        new ReturnValue("exception message",
                                WhatExceptionWithMessage.class.getCanonicalName(), ReturnValueType.REAL),
                        MethodExitType.EXCEPTION)
                )));

        SimplePojoA expectedValue = simpleServiceA.callToTest("a val", new SimplePojoB("1", 1));
//        System.out.println("Expected value: " + objectMapper.writeValueAsString(expectedValue));

        SimpleServiceA simpleServiceAWithMocks = (SimpleServiceA) agentCommandExecutor.arrangeMocks(
                simpleServiceA.getClass(),
                simpleServiceA.getClass().getClassLoader(),
                simpleServiceA, declaredMocks);


        try {
            SimplePojoA returnedValue = simpleServiceAWithMocks.callToTest("a val", new SimplePojoB("1", 1));
            Assertions.fail();
        } catch (Exception e) {
//            System.out.println("Exception class: " + e.getClass().getCanonicalName());
            Assertions.assertTrue(e instanceof WhatExceptionWithMessage);
            Assertions.assertEquals("exception message", e.getMessage());
        }
//        System.out.println("Mocked value: " + mockedResponseString);
        SimplePojoA originalValueAgain = simpleServiceAWithMocks.callToTest1("a val", new SimplePojoB("1", 1));
        String originalResponseString = objectMapper.writeValueAsString(originalValueAgain);
        Assertions.assertTrue(originalResponseString.contains("a val-991"));
        Assertions.assertTrue(originalResponseString.contains("7"));
//        System.out.println("Original value: " + originalResponseString);
    }
}