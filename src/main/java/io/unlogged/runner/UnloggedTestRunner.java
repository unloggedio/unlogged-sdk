package io.unlogged.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.unlogged.AgentCommandExecutorImpl;
import io.unlogged.AgentCommandRawResponse;
import io.unlogged.atomic.*;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;
import io.unlogged.logging.DiscardEventLogger;
import io.unlogged.mocking.DeclaredMock;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.*;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UnloggedTestRunner extends Runner {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(UnloggedTestRunner.class);
    final private Class<?> testClass;
    final private AtomicRecordService atomicRecordService = new AtomicRecordService();
    private final AgentCommandExecutorImpl commandExecutor;
    private final AtomicInteger testCounter = new AtomicInteger();
    private final TestContextManager springTestContextManager;
    private final DiscardEventLogger eventLogger = new DiscardEventLogger() {
        @Override
        public Object getObjectByClassName(String className) {
            ApplicationContext applicationContext =
                    springTestContextManager.getTestContext().getApplicationContext();
//            springTestContextManager.getTestContext()
//                    .getApplicationContext().getAutowireCapableBeanFactory()
//            springTestContextManager.getTestContext()
            try {
                return applicationContext.getBean(Class.forName(className));
            } catch (Throwable e) {
                // throws exception when spring cannot create the bean
                return null;
            }
        }
    };

    public UnloggedTestRunner(Class<?> testClass) {
        super();
        this.commandExecutor = new AgentCommandExecutorImpl(objectMapper, eventLogger);
        this.testClass = testClass;

//        TestContextBootstrapper bootstrap = BootstrapUtils.resolveTestContextBootstrapper(testClass);
//        TestContextBootstrapper bootstrap = BootstrapUtils.resolveTestContextBootstrapper(testClass);

//        BootstrapContext bootstrapContext = bootstrap.getBootstrapContext();
//        bootstrap.setBootstrapContext(new BootstrapContext() {
//            @Override
//            public Class<?> getTestClass() {
//                return bootstrapContext.getTestClass();
//            }
//
//            @Override
//            public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
//                return bootstrapContext.getCacheAwareContextLoaderDelegate();
//            }
//        });

//        this.springTestContextManager = new TestContextManager(testClass);
//        this.springTestContextManager.registerTestExecutionListeners(new TestExecutionListener() {
//            @Override
//            public void prepareTestInstance(TestContext testContext) throws Exception {
//                TestExecutionListener.super.prepareTestInstance(testContext);
//            }
//            @Override
//            public void beforeTestClass(TestContext testContext) throws Exception {
//                TestExecutionListener.super.beforeTestClass(testContext);
//            }
//        });
        this.springTestContextManager = new TestContextManager(testClass);

        this.springTestContextManager.registerTestExecutionListeners(new TestExecutionListener() {
            @Override
            public void beforeTestClass(TestContext testContext) throws Exception {
                TestExecutionListener.super.beforeTestClass(testContext);
            }
        });
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(testClass, "Unlogged test runner");
    }

    @Override
    public void run(RunNotifier notifier) {
        System.err.println("UTR.run invoked");

        try {
            Map<String, io.unlogged.runner.AtomicRecord> recordsMap = atomicRecordService.updateMap();
            Map<String, DeclaredMock> mocksById = recordsMap.values()
                    .stream()
                    .map(AtomicRecord::getDeclaredMockMap)
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(DeclaredMock::getId, e -> e));

            for (String classFileResource : recordsMap.keySet()) {
                AtomicRecord classRecords = recordsMap.get(classFileResource);
                Map<String, List<StoredCandidate>> storedCandidateMap = classRecords.getStoredCandidateMap();
//                System.err.println("running the tests from unlogged: " + classFileResource);
                for (String methodHashKey : storedCandidateMap.keySet()) {
                    List<StoredCandidate> candidates = storedCandidateMap.get(methodHashKey);
                    if (candidates.size() == 0) {
                        continue;
                    }

                    String className = candidates.get(0).getMethod().getClassName();
                    Description suiteDescription = Description.createSuiteDescription(className);
//                    System.err.println("running the tests from unlogged: " + className + "#" + methodHashKey);
                    notifier.fireTestSuiteStarted(suiteDescription);

                    for (StoredCandidate candidate : candidates) {
                        int testCounterIndex = testCounter.incrementAndGet();

                        Description testDescription = Description.createTestDescription(
                                Class.forName(candidate.getMethod().getClassName()), candidate.getName() == null ?
                                        candidate.getCandidateId() : candidate.getName()
                        );
                        notifier.fireTestStarted(testDescription);

                        fireTest(notifier, mocksById, candidate, testCounterIndex, testDescription);
                    }
                    notifier.fireTestSuiteFinished(suiteDescription);
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    private void fireTest(RunNotifier notifier, Map<String, DeclaredMock> mocksById, StoredCandidate candidate, int testCounterIndex, Description testDescription) {
        List<DeclaredMock> mockList = new ArrayList<>();
        List<String> mocksToUse = candidate.getMockIds();
        if (mocksToUse != null) {
            for (String mockId : mocksToUse) {
                DeclaredMock mockDefinition = mocksById.get(mockId);
                mockList.add(mockDefinition);
            }
        }


        AtomicAssertion assertions = candidate.getTestAssertions();
        AssertionResultWithRawObject verificationResultRaw = executeAndVerify(candidate, mockList);
        AgentCommandResponse agentCommandResponse = verificationResultRaw.getResponseObject().getAgentCommandResponse();
        AssertionResult verificationResult = verificationResultRaw.getAssertionResult();

        boolean isVerificationPassing = verificationResult.isPassing();
        System.out.println(
                "[#" + testCounterIndex + "] Candidate [" + candidate.getName() + "] => " + isVerificationPassing);

        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
            ((Throwable) verificationResultRaw.getResponseObject()
                    .getResponseObject()).printStackTrace();
        }
        if (isVerificationPassing) {
            notifier.fireTestFinished(testDescription);
            return;
        }

        if (agentCommandResponse == null) {
            notifier.fireTestFailure(new Failure(testDescription, new RuntimeException("Response is null")));
            return;
        }

        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
            notifier.fireTestFailure(new Failure(testDescription, (Throwable)
                    verificationResultRaw.getResponseObject().getResponseObject()));

        }

        List<AtomicAssertion> assertionList = AtomicAssertionUtils.flattenAssertionMap(assertions);
        AssertionResult assertionResultMap = verificationResultRaw.getAssertionResult();

        if (verificationResultRaw.getResponseObject() == null) {
            notifier.fireTestFailure(new Failure(testDescription,
                    new RuntimeException(String.valueOf(verificationResultRaw))));

        }

        AgentCommandRawResponse rawResponse = verificationResultRaw.getResponseObject();
        AgentCommandResponse acr = rawResponse.getAgentCommandResponse();
//            Object responseObject = rawResponse.getResponseObject();

        for (AtomicAssertion atomicAssertion : assertionList) {
            Boolean status = assertionResultMap.getResults().get(atomicAssertion.getId());
            if (status) {
                continue;
            }

            JsonNode objectNode;
            String methodReturnValue = (String) acr.getMethodReturnValue();
            try {
                objectNode = objectMapper.readTree(methodReturnValue);
            } catch (Exception e) {
                objectNode = JsonNodeFactory.instance.textNode(methodReturnValue);
            }

            Object valueFromJsonNode = JsonTreeUtils.getValueFromJsonNode(objectNode, atomicAssertion.getKey());
            RuntimeException thrownException = new RuntimeException("Expected" +
                    " [" + atomicAssertion.getExpectedValue() + "] instead of" +
                    " actual" + " [" + valueFromJsonNode + "]");
            Failure failure = new Failure(testDescription, thrownException);
            notifier.fireTestFailure(failure);


        }


    }

    private AssertionResultWithRawObject executeAndVerify(StoredCandidate candidate, List<DeclaredMock> mockList) {
        AgentCommandRawResponse executionResult = executeCandidate(candidate, mockList);
        if (executionResult == null) {
            AssertionResult assertionResult = new AssertionResult();
            assertionResult.setPassing(false);
            return new AssertionResultWithRawObject(assertionResult,
                    new AgentCommandRawResponse(new AgentCommandResponse(),
                            new Exception("execution result is null")));
        }
        AssertionResult assertionResult = verifyCandidateExecution(executionResult.getAgentCommandResponse(),
                candidate);
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
        return AssertionEngine.executeAssertions(candidate.getTestAssertions(), getResponseNode(
                (String) executionResult.getMethodReturnValue(), executionResult.getResponseClassName()
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
            e.printStackTrace();
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
                    throw new RuntimeException(e1);
                }
            }
        }
        return null;
    }

}