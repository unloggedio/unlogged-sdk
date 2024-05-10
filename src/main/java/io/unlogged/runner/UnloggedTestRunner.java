package io.unlogged.runner;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.unlogged.AgentCommandExecutorImpl;
import io.unlogged.AgentCommandRawResponse;
import io.unlogged.MethodSignatureParser;
import io.unlogged.Runtime;
import io.unlogged.atomic.*;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;
import io.unlogged.logging.DiscardEventLogger;
import io.unlogged.logging.ObjectMapperFactory;
import io.unlogged.mocking.DeclaredMock;
import junit.framework.AssertionFailedError;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UnloggedTestRunner extends Runner {

    private static final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(UnloggedTestRunner.class);
    private static final List<String> JACKSON_PROPERTY_NAMES_SET_FALSE = Arrays.asList(
            "FAIL_ON_UNKNOWN_PROPERTIES",
            "FAIL_ON_IGNORED_PROPERTIES",
            "FAIL_ON_NULL_FOR_PRIMITIVES",
            "FAIL_ON_NULL_CREATOR_PROPERTIES",
            "FAIL_ON_MISSING_CREATOR_PROPERTIES",
            "FAIL_ON_NUMBERS_FOR_ENUMS",
            "FAIL_ON_TRAILING_TOKENS"
    );
    private static boolean isLombokPresent;

    static {
        System.setProperty("UNLOGGED_DISABLE", "true");
        try {
            Class<?> lombokBuilderAnnotation = Class.forName("lombok.Builder");
            isLombokPresent = true;
//            System.err.println("Lombok found: " + lombokBuilderAnnotation.getCanonicalName());
        } catch (ClassNotFoundException e) {
//            System.err.println("Lombok not found");
            isLombokPresent = false;
        }

        objectMapper = ObjectMapperFactory.createObjectMapperReactive();

    }

    final private AtomicRecordService atomicRecordService = new AtomicRecordService();
    private final AgentCommandExecutorImpl commandExecutor;
    private final AtomicInteger testCounter = new AtomicInteger();
    private boolean isSpringPresent;
    private Method getBeanMethod;
    private Object applicationContext;
    private final Description testDescription;
    private final Map<String, Description> descriptionMap = new HashMap<>();
    private final Map<Description, StoredCandidate> descriptionStoredCandidateMap = new HashMap<>();
    //    private Object springTestContextManager;

    public UnloggedTestRunner(Class<?> testClass) {
        super();
        // throws exception when spring cannot create the bean
        DiscardEventLogger eventLogger = new DiscardEventLogger() {
            @Override
            public Object getObjectByClassName(String className) {
                if (applicationContext == null) {
                    return null;
                }
                try {
                    return getBeanMethod.invoke(applicationContext, Class.forName(className));
                } catch (Throwable e) {
                    // throws exception when spring cannot create the bean
                    return null;
                }
            }
        };
        this.commandExecutor = new AgentCommandExecutorImpl(ObjectMapperFactory.createObjectMapperReactive(),
                eventLogger);
        this.testDescription = Description.createTestDescription(testClass, "Unlogged test runner");

        commandExecutor.enableSpringIntegration(testClass);
        collectTests();
        Runtime.getInstance("format=discard");
    }

    @Override
    public Description getDescription() {
        logger.debug("getDescription: " + testDescription);
        return testDescription;
    }

    @Override
    public void run(RunNotifier notifier) {

        Map<String, io.unlogged.runner.AtomicRecord> recordsMap = atomicRecordService.updateMap();
        Map<String, DeclaredMock> mocksById = recordsMap.values()
                .stream()
                .map(AtomicRecord::getDeclaredMockMap)
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(DeclaredMock::getId, e -> e));

//        logger.warn("testStart: " + testDescription);
//        notifier.fireTestStarted(testDescription);
        for (Description description : testDescription.getChildren()) {
            executeByDescription(notifier, mocksById, description);
        }
//        notifier.fireTestFinished(testDescription);
//        logger.warn("fireTestFinished: " + testDescription);
    }

    private void executeByDescription(RunNotifier notifier, Map<String, DeclaredMock> mocksById, Description description) {
        StoredCandidate candidate = descriptionStoredCandidateMap.get(description);
        if (candidate != null) {
            notifier.fireTestStarted(description);
            Throwable throwable = fireTest(mocksById, candidate);
            if (throwable != null) {
                notifier.fireTestFailure(new Failure(description, throwable));
            }
        }

        if (description.getChildren() != null) {
            for (Description child : description.getChildren()) {
                executeByDescription(notifier, mocksById, child);
            }
        }
        if (candidate != null) {
            notifier.fireTestFinished(description);
        }

    }

    public void collectTests() {
        descriptionStoredCandidateMap.clear();

        try {
            Map<String, io.unlogged.runner.AtomicRecord> recordsMap = atomicRecordService.updateMap();


            for (String className : recordsMap.keySet()) {
                AtomicRecord classRecords = recordsMap.get(className);
                Map<String, List<StoredCandidate>> storedCandidateMap = classRecords.getStoredCandidateMap();

                int candidateCount = storedCandidateMap.values().stream().mapToInt(Collection::size).sum();
                if (candidateCount < 1) {
                    continue;
                }
                Class<?> testClass1;
                try {
                    testClass1 = Class.forName(className);
                } catch (ClassNotFoundException classNotFoundException) {
                    System.err.println("Class not found: " + className);
                    continue;
                }
                Description suiteDescription = Description.createSuiteDescription(testClass1);

                testDescription.addChild(suiteDescription);

                for (String methodHashKey : storedCandidateMap.keySet()) {
                    List<StoredCandidate> candidates = storedCandidateMap.get(methodHashKey);
                    if (candidates.size() == 0) {
                        continue;
                    }

                    for (StoredCandidate candidate : candidates) {

                        Class<?> targetClassTypeInstance;
                        String candidateId = candidate.getName() == null ?
                                candidate.getCandidateId() : candidate.getName();
                        targetClassTypeInstance = Class.forName(candidate.getMethod().getClassName());

                        Description testDescription = getTestDescription(targetClassTypeInstance, candidateId,
                                candidate.getCandidateId());

                        suiteDescription.addChild(testDescription);
//                        this.testDescription.addChild(testDescription);
                        descriptionStoredCandidateMap.put(testDescription, candidate);

                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    private Description getTestDescription(Class<?> targetClassTypeInstance, String name, String id) {
        String key = targetClassTypeInstance.getCanonicalName() + name + id;
        if (descriptionMap.containsKey(key)) {
            return descriptionMap.get(key);
        }
        Description testDescription1 = null;
        try {
            testDescription1 = Description.createTestDescription(
                    Class.forName(targetClassTypeInstance.getCanonicalName()), name);
        } catch (ClassNotFoundException e) {
            testDescription1 = Description.createTestDescription(targetClassTypeInstance.getCanonicalName(),
                    name, id);
        }
        descriptionMap.put(key, testDescription1);
        return testDescription1;
    }

    private Throwable fireTest(
            Map<String, DeclaredMock> mocksById, StoredCandidate candidate) {
        MethodUnderTest methodUnderTest = candidate.getMethod();
        List<DeclaredMock> mockList = new ArrayList<>();
        List<String> mocksToUse = candidate.getMockIds();
        if (mocksToUse != null) {
            for (String mockId : mocksToUse) {
                DeclaredMock mockDefinition = mocksById.get(mockId);
                if (mockDefinition == null) {
                    return new UndeclaredThrowableException(
                            new Throwable("Could not find mock " + mockId + " used in candidate: " + candidate.getCandidateId()),
                            "Could not find mock " + mockId + " used in candidate: " + candidate.getCandidateId()
                    );
                }
                mockList.add(mockDefinition);
            }
        }


        AtomicAssertion assertions = candidate.getTestAssertions();
        AssertionResultWithRawObject verificationResultRaw = executeAndVerify(candidate, mockList);
        AgentCommandResponse agentCommandResponse = verificationResultRaw.getResponseObject().getAgentCommandResponse();
        AssertionResult verificationResult = verificationResultRaw.getAssertionResult();

        boolean isVerificationPassing = verificationResult.isPassing();
//        System.out.println(
//                "[#" + candidate.getName() + "] tested method [" + candidate.getMethod()
//                        .getName() + "] => " + isVerificationPassing);

//        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
//            ((Throwable) verificationResultRaw.getResponseObject()
//                    .getResponseObject()).printStackTrace();
//        }
        if (isVerificationPassing) {
            return null;
        }

        if (agentCommandResponse == null) {
//            System.out.println("Response is null");
//            notifier.fireTestFailure();
            return new AssertionFailedError("Response is null");
        }

        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
//            Throwable responseObject = (Throwable) verificationResultRaw.getResponseObject().getResponseObject();
//            System.out.println("Method [" + candidate.getMethod().getName() + "] threw an exception");
//            responseObject.printStackTrace(System.err);
//            notifier.fireTestFailure(new Failure(testDescription, (Throwable)
//                    verificationResultRaw.getResponseObject().getResponseObject()));
            return (Throwable) verificationResultRaw.getResponseObject().getResponseObject();
        }

        List<AtomicAssertion> assertionList = AtomicAssertionUtils.flattenAssertionMap(assertions);
        AssertionResult assertionResultMap = verificationResultRaw.getAssertionResult();

        if (verificationResultRaw.getResponseObject() == null) {
//            System.out.println("Response is null");
            return new AssertionFailedError(String.valueOf(verificationResultRaw));

        }

        AgentCommandRawResponse rawResponse = verificationResultRaw.getResponseObject();
        AgentCommandResponse acr = rawResponse.getAgentCommandResponse();
        Object responseObject = rawResponse.getResponseObject();
        if (responseObject instanceof Throwable) {
//            System.out.println("Method threw an exception");
//            ((Throwable) responseObject).printStackTrace();
        }

        for (AtomicAssertion atomicAssertion : assertionList) {
            Boolean status = assertionResultMap.getResults().get(atomicAssertion.getId());
            if (status) {
                continue;
            }

            if (atomicAssertion.getAssertionType() == AssertionType.ANYOF
                    || atomicAssertion.getAssertionType() == AssertionType.ALLOF
                    || atomicAssertion.getAssertionType() == AssertionType.NOTANYOF
                    || atomicAssertion.getAssertionType() == AssertionType.NOTALLOF
            ) {
                continue;
            }

            JsonNode objectNode;
            String methodReturnValue = acr.getMethodReturnValue() instanceof String ?
                    (String) acr.getMethodReturnValue() : String.valueOf(acr.getMethodReturnValue());
            if (methodReturnValue == null) {
                try {
                    methodReturnValue = objectMapper.writeValueAsString(responseObject);
                } catch (JsonProcessingException e) {
                    methodReturnValue = String.valueOf(responseObject);
                    // thats all we can try
                }
            }
            try {
                objectNode = objectMapper.readTree(methodReturnValue);
            } catch (Exception e) {
                objectNode = objectMapper.getNodeFactory().textNode(methodReturnValue);
            }


            JsonNode valueFromJsonNode = JsonTreeUtils.getValueFromJsonNode(objectNode, atomicAssertion.getKey());
            Expression expression = atomicAssertion.getExpression();
            JsonNode expressedValue = expression.compute(valueFromJsonNode);

//            System.err.println("Expected [" + atomicAssertion.getExpectedValue() + "] instead of actual " +
//                    "[" + expressedValue + "]\n\t when the return value from method " +
//                    "[" + methodUnderTest.getName() + "()]\n\t value " +
//                    "[" + (expression == Expression.SELF ? atomicAssertion.getKey() : (expression.name() +
//                    "(" + atomicAssertion.getKey() + ")")) +
//                    "] as expected in test candidate " +
//                    "[" + candidate.getCandidateId() + "]" +
//                    "[" + candidate.getName() + "]");
            String className = methodUnderTest.getClassName();
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf(".") + 1);
            }
            String message = "Expected " + atomicAssertion.getExpression() + "([" + atomicAssertion.getExpectedValue() + "]) " +
                    atomicAssertion.getAssertionType().toString() + "  actual " +
                    "[" + expressedValue + "]\n\t when the return value from method " +
                    "[" + className + "." + methodUnderTest.getName() + methodUnderTest.getSignature() + "]\n\t value " +
                    "[" + (expression == Expression.SELF ? atomicAssertion.getKey() : (expression.name() +
                    "(" + atomicAssertion.getKey() + ")")) +
                    "] as expected in test candidate " +
                    "[" + candidate.getCandidateId() + "]" +
                    "[" + candidate.getName() + "]";


            List<String> methodParameterTypes = MethodSignatureParser.parseMethodSignature(
                    methodUnderTest.getSignature());

            List<Object> messageTemplateValues = new ArrayList<>();


            messageTemplateValues.add(className + "." + methodUnderTest.getName() + "()");
            messageTemplateValues.add(candidate.getCandidateId());
            int parameterCount = methodParameterTypes.size() - 1;
            messageTemplateValues.add(methodParameterTypes.get(parameterCount));
            messageTemplateValues.add(candidate.getMockIds().size());

            String messageTemplate = "\n\n" +
                    "┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐\n" +
                    "│ %-91s %-36s │\n" +
                    "│                                                                                                                                  │\n" +
                    "│ Returns: %-120s│\n" +
                    "│                                                                                                                                  │\n" +
                    "│ Mocks: %-122s│\n" +
                    "│                                                                                                                                  │\n" +
                    "│ Parameters: %-117s│\n" +
                    "│                                                                                                                                  │\n";

            messageTemplateValues.add(parameterCount);
            for (int i = 0; i < parameterCount; i++) {
                String paramType = methodParameterTypes.get(i);
                messageTemplate = messageTemplate +
                        "│   %-127s│\n";
                messageTemplateValues.add(paramType + ": " + candidate.getMethodArguments().get(i));
            }
            if (parameterCount > 0) {
                messageTemplate +=
                        "│                                                                                                                                  │\n"
                ;
            }

            messageTemplate = messageTemplate +
                    "│ Failed Assertion                                                                                                                 │\n" +
                    "│                                                                                                                                  │\n" +
                    "│  Key:            %-112s│\n" +
                    "│  Expression:     %-112s│\n" +
                    "│  Assertion:      %-112s│\n" +
                    "│  Expected Value: %-112s│\n" +
                    "│  Actual Value:   %-112s│\n" +
                    "│                                                                                                                                  │\n" +
                    "│                                                                                                                                  │\n" +
                    "│ %-88s                UnloggedReplayTestReport │\n" +
                    "└──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘";


            messageTemplateValues.add(atomicAssertion.getKey());
            messageTemplateValues.add(atomicAssertion.getExpression());
            messageTemplateValues.add(atomicAssertion.getAssertionType());
            messageTemplateValues.add(atomicAssertion.getExpectedValue());
            messageTemplateValues.add(expressedValue);
            messageTemplateValues.add(new Date().toString());


            AssertionFailedError thrownException = new AssertionFailedError(
                    String.format(messageTemplate, messageTemplateValues.toArray(new Object[0]))
            );
//            Failure failure = new Failure(testDescription, thrownException);
//            throw thrownException;
            return thrownException;
//            notifier.fireTestAssumptionFailed(failure);
        }


        return null;
    }


    private AssertionResultWithRawObject executeAndVerify(StoredCandidate candidate, List<DeclaredMock> mockList) {

        List<String> methodArgumentValues = candidate.getMethodArguments();
        ArrayList<String> newArgumentValues = new ArrayList<>(methodArgumentValues.size());
        List<String> methodSignatureTypes = MethodSignatureParser.parseMethodSignature(
                candidate.getMethod().getSignature());
        // remove the return type
        String methodReturnType = methodSignatureTypes.remove(methodSignatureTypes.size() - 1);
        boolean processReturnValueAsFloatDouble = Objects.equals(methodReturnType, "F")
                || Objects.equals(methodReturnType, "f")
                || Objects.equals(methodReturnType, "java.lang.Float")
                || Objects.equals(methodReturnType, "Ljava/lang/Float;")
                || Objects.equals(methodReturnType, "Ljava/lang/Double;")
                || Objects.equals(methodReturnType, "java.lang.Double")
                || Objects.equals(methodReturnType, "D");
        for (int i = 0; i < methodSignatureTypes.size(); i++) {
            String newValue = methodArgumentValues.get(i);
            String methodSignatureType = methodSignatureTypes.get(i);
            switch (methodSignatureType) {
                case "F":
                case "f":
                case "java.lang.Float":
                case "Ljava/lang/Float;":
                case "D":
                case "d":
                case "java.lang.Double":
                case "Ljava/lang/Double;":
                    newValue = ParameterUtils.processResponseForFloatAndDoubleTypes(methodSignatureType, newValue);
                    newArgumentValues.add(newValue);
                    break;
                default:
                    newArgumentValues.add(newValue);
            }

        }
        candidate.setMethodArguments(newArgumentValues);


        AgentCommandRawResponse executionResult = executeCandidate(candidate, mockList);
        if (executionResult.getResponseObject() instanceof Exception) {
            ((Exception) executionResult.getResponseObject()).printStackTrace();
        }
        AgentCommandResponse acr = executionResult.getAgentCommandResponse();
        if (acr != null) {
            if (processReturnValueAsFloatDouble) {
                Object returnValue = acr.getMethodReturnValue();
                String processedReturnValue = ParameterUtils.processResponseForFloatAndDoubleTypes(methodReturnType,
                        String.valueOf(returnValue));
                acr.setMethodReturnValue(processedReturnValue);
            }
        }

        if (executionResult == null) {
            AssertionResult assertionResult = new AssertionResult();
            assertionResult.setPassing(false);
            return new AssertionResultWithRawObject(assertionResult,
                    new AgentCommandRawResponse(new AgentCommandResponse(),
                            new AssertionFailedError("execution result is null")));
        }
        AssertionResult assertionResult;
        try {
            assertionResult = verifyCandidateExecution(executionResult.getAgentCommandResponse(),
                    candidate);
        } catch (AssertionFailedError assertionFailedError) {
            assertionResult = new AssertionResult();
            assertionResult.setPassing(false);
            return new AssertionResultWithRawObject(assertionResult,
                    new AgentCommandRawResponse(new AgentCommandResponse(), assertionFailedError));
        }
        return new AssertionResultWithRawObject(assertionResult, executionResult);
//        return assertionResult;
    }

    private AssertionResult verifyCandidateExecution(AgentCommandResponse executionResult, StoredCandidate candidate) {
        if (executionResult == null) {
            logger.warn("response is null [" + candidate + "]");
            AssertionResult assertionResult = new AssertionResult();
            assertionResult.setPassing(false);
            return assertionResult;
        }
        String responseClassName = executionResult.getResponseClassName();

        List<String> params = MethodSignatureParser.parseMethodSignature(candidate.getMethod().getSignature());
        responseClassName = params.get(params.size() - 1);


        return AssertionEngine.executeAssertions(
                candidate.getTestAssertions(),
                getResponseNode(String.valueOf(executionResult.getMethodReturnValue()),
                        responseClassName
                ));
    }

    private AgentCommandRawResponse executeCandidate(StoredCandidate candidate, List<DeclaredMock> mockList) {
        try {
            MethodUnderTest methodUnderTest = candidate.getMethod();

            ClassUnderTest classUnderTest = new ClassUnderTest(methodUnderTest.getClassName());
            AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(methodUnderTest,
                    classUnderTest, candidate.getMethodArguments(), true);
            if (agentCommandRequest == null) {
                return new AgentCommandRawResponse(new AgentCommandResponse(), new Exception("Failed to create " +
                        "request for candidate [" + candidate + "]"));
            }
            agentCommandRequest.setDeclaredMocks(mockList);

            logger.info("Execute candidate: " + agentCommandRequest);

            return commandExecutor.executeCommandRaw(agentCommandRequest);

        } catch (Exception e) {
//            e.printStackTrace();
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            return new AgentCommandRawResponse(agentCommandResponse, e);
        }
    }

    private JsonNode getResponseNode(String methodReturnValue, String responseClassName) {
        try {
            if (methodReturnValue == null) {
                return JsonNodeFactory.instance.nullNode();
            }

            if (responseClassName.equals("char") || responseClassName.equals("C")) {
                try {
                    methodReturnValue =
                            String.valueOf(objectMapper.readTree(methodReturnValue).textValue().codePointAt(0));
                    return objectMapper.getNodeFactory().numberNode(Integer.valueOf(methodReturnValue));
                } catch (JsonProcessingException e) {
                    //
                }
            }

            return objectMapper.readTree(methodReturnValue);
        } catch (JsonProcessingException e) {
            // this shouldn't happen
            if ("java.lang.String".equals(responseClassName)
                    && !methodReturnValue.startsWith("\"")
                    && !methodReturnValue.endsWith("\"")) {
                try {
                    return objectMapper.readTree("\"" + methodReturnValue + "\"");
                } catch (JsonProcessingException e1) {
                    // failed to read as a json node
                    throw new AssertionFailedError("Failed to parse response: " + methodReturnValue
                            + " => " + e1.getMessage());
                }
            }
        }
        return null;
    }

    private static class DummyClosable implements Closeable {

        @Override
        public void close() throws IOException {

        }
    }

}