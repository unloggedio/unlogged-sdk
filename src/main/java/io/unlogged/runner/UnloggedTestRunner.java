package io.unlogged.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.unlogged.AgentCommandExecutorImpl;
import io.unlogged.AgentCommandRawResponse;
import io.unlogged.atomic.AssertionEngine;
import io.unlogged.atomic.AssertionResult;
import io.unlogged.atomic.MethodUnderTest;
import io.unlogged.atomic.StoredCandidate;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;
import io.unlogged.logging.DiscardEventLogger;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UnloggedTestRunner extends Runner {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(UnloggedTestRunner.class);
    final private Class<?> testClass;
    final private AtomicRecordService atomicRecordService = new AtomicRecordService();
    private final AgentCommandExecutorImpl commandExecutor;
    private final AtomicInteger testCounter = new AtomicInteger();

    public UnloggedTestRunner(Class<?> testClass) {
        super();
        this.commandExecutor = new AgentCommandExecutorImpl(objectMapper, new DiscardEventLogger());
        this.testClass = testClass;
    }

    @Override
    public Description getDescription() {
        return Description
                .createTestDescription(testClass, "My runner description");
    }

    @Override
    public void run(RunNotifier notifier) {
        System.err.println("UTR.run invoked");

        try {


            Map<String, io.unlogged.runner.AtomicRecord> recordsMap = atomicRecordService.updateMap();

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
//                    System.err.println("running the tests from unlogged: " + testCounterIndex);
                        Description testDescription = Description.createTestDescription(
                                candidate.getMethod().getClassName(), candidate.getName() == null ?
                                        candidate.getCandidateId() : candidate.getName(), candidate.getCandidateId()
                        );
                        notifier.fireTestStarted(testDescription);

                        AssertionResultWithRawObject verificationResultRaw = executeAndVerify(candidate);
                        AgentCommandResponse agentCommandResponse = verificationResultRaw.getResponseObject()
                                .getAgentCommandResponse();
                        AssertionResult verificationResult = verificationResultRaw.getAssertionResult();

                        System.out.println("[#" + testCounterIndex + "] Candidate [" + candidate.getName() + "] => " +
                                verificationResult.isPassing());
                        try {
                            if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
                                ((Throwable) verificationResultRaw.getResponseObject()
                                        .getResponseObject()).printStackTrace();
                            }
                            if (verificationResult.isPassing()) {
                                notifier.fireTestFinished(testDescription);
                            } else {
                                if (agentCommandResponse != null) {
                                    if (agentCommandResponse.getResponseType() == ResponseType.EXCEPTION) {
                                        Failure testFailure = new Failure(testDescription,
                                                (Throwable) verificationResultRaw.getResponseObject()
                                                        .getResponseObject());
                                        notifier.fireTestFailure(testFailure);
                                    } else if (agentCommandResponse.getResponseType() == ResponseType.FAILED) {
                                        notifier.fireTestFailure(new Failure(testDescription,
                                                new RuntimeException(agentCommandResponse.getMessage())));
                                    } else {
                                        notifier.fireTestFailure(new Failure(testDescription,
                                                new RuntimeException(String.valueOf(
                                                        verificationResultRaw.getResponseObject()
                                                                .getResponseObject()))));
                                    }

                                } else {
                                    notifier.fireTestFailure(
                                            new Failure(testDescription, new RuntimeException("Response is null")));

                                }
                            }
                            notifier.fireTestFinished(testDescription);
//                        notifier.fire(result);
                        } catch (Exception e) {
                            notifier.fireTestFailure(new Failure(testDescription, e));
                            throw new RuntimeException(e);
                        }
                    }
                    notifier.fireTestSuiteFinished(suiteDescription);
                }
            }
        }catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        }
    }

    private AssertionResultWithRawObject executeAndVerify(StoredCandidate candidate) {
        AgentCommandRawResponse executionResult = executeCandidate(candidate);
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

    private AgentCommandRawResponse executeCandidate(StoredCandidate candidate) {
        try {
            MethodUnderTest methodUnderTest = candidate.getMethod();

            ClassUnderTest classUnderTest = new ClassUnderTest(methodUnderTest.getClassName());
            AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(methodUnderTest,
                    classUnderTest, candidate.getMethodArguments(), true);

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