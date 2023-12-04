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
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import io.unlogged.AgentCommandExecutorImpl;
import io.unlogged.AgentCommandRawResponse;
import io.unlogged.Runtime;
import io.unlogged.atomic.*;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandResponse;
import io.unlogged.command.ResponseType;
import io.unlogged.logging.DiscardEventLogger;
import io.unlogged.mocking.DeclaredMock;
import io.unlogged.util.ClassTypeUtil;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        try {
            Class<?> lombokBuilderAnnotation = Class.forName("lombok.Builder");
            isLombokPresent = true;
//            System.err.println("Lombok found: " + lombokBuilderAnnotation.getCanonicalName());
        } catch (ClassNotFoundException e) {
//            System.err.println("Lombok not found");
            isLombokPresent = false;
        }

        objectMapper = createObjectMapper();

    }

    final private Class<?> testClass;
    final private AtomicRecordService atomicRecordService = new AtomicRecordService();
    private final AgentCommandExecutorImpl commandExecutor;
    private final AtomicInteger testCounter = new AtomicInteger();
    private boolean isSpringPresent;
    private Method getBeanMethod;
    private Object applicationContext;
    private final DiscardEventLogger eventLogger = new DiscardEventLogger() {
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
//    private Object springTestContextManager;

    public UnloggedTestRunner(Class<?> testClass) {
        super();
        this.commandExecutor = new AgentCommandExecutorImpl(objectMapper, eventLogger);
        this.testClass = testClass;

        Runtime.getInstance("format=discard");
    }

    public static ObjectMapper createObjectMapper() {
        // For 2.13.1
        // Load JsonMappingException class force load so that we don't get a StackOverflow when we are in a cycle
        JsonMappingException jme = new JsonMappingException(new DummyClosable(), "load class");
        jme.prependPath(new JsonMappingException.Reference("from dummy"));
        JsonMapper.Builder jacksonBuilder = JsonMapper.builder();

        for (DeserializationFeature value : DeserializationFeature.values()) {
            if (JACKSON_PROPERTY_NAMES_SET_FALSE.contains(value.name())) {
                jacksonBuilder.configure(value, false);
            }
        }

        jacksonBuilder.annotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public boolean hasIgnoreMarker(AnnotatedMember m) {
                String fullName = m.getFullName();
                if (m.getDeclaringClass().getCanonicalName().contains("_$$_")) {
                    return true;
                }
                String rawTypeCanonicalName = m.getRawType().getCanonicalName();
                if (rawTypeCanonicalName.equals("javassist.util.proxy.MethodHandler")) {
                    return true;
                }
                if (fullName.contains(".$Proxy")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("java.lang.Thread")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("java.util.function.")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("java.lang.reflect.")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("jdk.internal.reflect.")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("io.mongock.")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("sun.reflect.")) {
                    return true;
                }
                if (rawTypeCanonicalName.equals("sun.nio.ch.Interruptible")) {
                    return true;
                }
                if (rawTypeCanonicalName.equals("java.security.AccessControlContext")) {
                    return true;
                }
                if (rawTypeCanonicalName.equals("java.lang.ClassLoader")) {
                    return true;
                }
                if (rawTypeCanonicalName.equals("java.lang.Runnable")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("reactor.core.")) {
                    return true;
                }
                if (fullName.startsWith("reactor.")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("io.netty.resolver")) {
                    return true;
                }
                if (rawTypeCanonicalName.startsWith("org.reactivestreams.")) {
                    return true;
                }
//                    System.out.println("hasIgnoreMarker: " + m.getFullName() + " => " + rawTypeCanonicalName);
                return false;
            }

            @Override
            public Object findSerializer(Annotated a) {
                if (Objects.equals(a.getRawType(), Date.class)) {
                    return null;
                }
                return super.findSerializer(a);
            }

            @Override
            public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
//                    System.err.println("Find POJO builder config: " + ac.getName());
                if (ac.hasAnnotation(
                        JsonPOJOBuilder.class)) {//If no annotation present use default as empty prefix
                    return super.findPOJOBuilderConfig(ac);
                }
                return new JsonPOJOBuilder.Value("build", "");
            }

            @Override
            public Class<?> findPOJOBuilder(AnnotatedClass ac) {
//                    System.err.println("Find POJO builder: " + ac.getName());

//                    if (isLombokPresent) {
//                        System.err.println("Annotation found: " + ac.hasAnnotation(lombokBuilderAnnotation));
//                    }

                if (ac.getRawType().getCanonicalName().startsWith("java.")) {
                    return null;
                }

                if (isLombokPresent) {
                    try {
                        String classFullyQualifiedName = ac.getName();
                        String classSimpleName = classFullyQualifiedName.substring(
                                classFullyQualifiedName.lastIndexOf(".") + 1);
                        String lombokClassBuilderName = ac.getName() + "$" + classSimpleName + "Builder";
//                            System.err.println("Lookup builder by nameclean: " + lombokClassBuilderName);
                        if (ac.getRawType().getClassLoader() != null) {
                            return ac.getRawType().getClassLoader().loadClass(lombokClassBuilderName);
                        } else {
                            return getClass().getClassLoader().loadClass(lombokClassBuilderName);
                        }
                    } catch (ClassNotFoundException e) {
                        return super.findPOJOBuilder(ac);
                    }
                }
                return super.findPOJOBuilder(ac);
            }
        });
        DateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");
        jacksonBuilder.defaultDateFormat(df);
        jacksonBuilder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        jacksonBuilder.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        try {
            Field fieldWriteSelfReferencesAsNull = SerializationFeature.class.getDeclaredField(
                    "WRITE_SELF_REFERENCES_AS_NULL");
            // field found
            jacksonBuilder.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        } catch (NoSuchFieldException e) {
            // no field WRITE_SELF_REFERENCES_AS_NULL
        }


        try {
            Class.forName("javax.persistence.ElementCollection");
            Class<?> hibernateClassPresent = Class.forName("org.hibernate.SessionFactory");
            Class<?> hibernateModule = Class.forName(
                    "com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module");
            com.fasterxml.jackson.databind.Module module = (com.fasterxml.jackson.databind.Module) hibernateModule.getDeclaredConstructor()
                    .newInstance();
            Class<?> featureClass = Class.forName(
                    "com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module$Feature");
            Method configureMethod = hibernateModule.getMethod("configure", featureClass, boolean.class);
            configureMethod.invoke(module, featureClass.getDeclaredField("FORCE_LAZY_LOADING").get(null), true);
            configureMethod.invoke(module,
                    featureClass.getDeclaredField("REPLACE_PERSISTENT_COLLECTIONS").get(null), true);
            configureMethod.invoke(module, featureClass.getDeclaredField("USE_TRANSIENT_ANNOTATION").get(null),
                    false);
            jacksonBuilder.addModule(module);
//                System.out.println("Loaded hibernate module");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
//                e.printStackTrace();
//                System.out.println("Failed to load hibernate module: " + e.getMessage());
            // hibernate module not found
            // add a warning in System.err here ?
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // potentially
//            jacksonBuilder.findAndAddModules();
        List<String> jacksonModules = Arrays.asList(
                "com.fasterxml.jackson.datatype.jdk8.Jdk8Module",
                "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
                "com.fasterxml.jackson.datatype.joda.JodaModule",
//                        "com.fasterxml.jackson.module.blackbird.BlackbirdModule",
                "com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule",
                "com.fasterxml.jackson.module.mrbean.MrBeanModule",
//                        "com.fasterxml.jackson.module.afterburner.AfterburnerModule",
                "com.fasterxml.jackson.module.paranamer.ParanamerModule",
                "software.fitz.jackson.module.force.ForceDeserializerModule"
        );
        for (String jacksonModule : jacksonModules) {
            try {
                //checks for presence of this module class, if not present throws exception
                Class<?> jdk8Module = Class.forName(jacksonModule);
                jacksonBuilder.addModule((Module) jdk8Module.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                // jdk8 module not found
            } catch (InvocationTargetException
                     | InstantiationException
                     | IllegalAccessException
                     | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Class<?> kotlinModuleClass = Class.forName("com.fasterxml.jackson.module.kotlin.KotlinModule");
            KotlinModule kotlinModule = new KotlinModule.Builder().build();
            jacksonBuilder.addModule(kotlinModule);
        } catch (ClassNotFoundException e) {
            // kotlin module for jackson not present on classpath
        }


        JsonMapper objectMapperInstance = jacksonBuilder.build();
        objectMapperInstance.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapperInstance;
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(testClass, "Unlogged test runner");
    }

    @Override
    public void run(RunNotifier notifier) {
//        System.err.println("UTR.run invoked");

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

                    MethodUnderTest method = candidates.get(0).getMethod();
                    String className = method.getClassName();
                    if (className.contains(".")) {
                        className = className.substring(className.lastIndexOf(".") + 1);
                    }
                    Description suiteDescription = Description.createSuiteDescription(
                            className + "." + method.getName());
//                    System.err.println(className );
                    try {
                        notifier.fireTestSuiteStarted(suiteDescription);
                    } catch (NoSuchMethodError ingnored) {
                        // ingnored
                    }

                    for (StoredCandidate candidate : candidates) {
                        int testCounterIndex = testCounter.incrementAndGet();

                        Class<?> targetClassTypeInstance;
                        String name = candidate.getName() == null ?
                                candidate.getCandidateId() : candidate.getName();
                        targetClassTypeInstance = Class.forName(candidate.getMethod().getClassName());

//                        if (isSpringPresent) {
//                        } else {
//                            targetClassTypeInstance = Class.forName(candidate.getMethod().getClassName());
//                        }

                        Description testDescription = Description.createTestDescription(
                                targetClassTypeInstance, name);

                        notifier.fireTestStarted(testDescription);

                        fireTest(notifier, mocksById, candidate, testDescription);

                        notifier.fireTestFinished(testDescription);
//                        if (springTestContextManager != null) {
//                            ((TestContextManager) springTestContextManager).afterTestMethod();
//                        }
                    }
                    try {
                        notifier.fireTestSuiteFinished(suiteDescription);
                    } catch (NoSuchMethodError ingnored) {
                        // ingnored
                    }

                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    private void fireTest(RunNotifier notifier, Map<String, DeclaredMock> mocksById, StoredCandidate candidate, Description testDescription) {
        MethodUnderTest methodUnderTest = candidate.getMethod();
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
                "[#" + candidate.getName() + "] tested method [" + candidate.getMethod()
                        .getName() + "] => " + isVerificationPassing);

        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
            ((Throwable) verificationResultRaw.getResponseObject()
                    .getResponseObject()).printStackTrace();
        }
        if (isVerificationPassing) {
            return;
        }

        if (agentCommandResponse == null) {
            System.out.println("Response is null");
            notifier.fireTestFailure(new Failure(testDescription, new RuntimeException("Response is null")));
            return;
        }

        if (verificationResultRaw.getResponseObject().getResponseObject() instanceof Throwable) {
            Throwable responseObject = (Throwable) verificationResultRaw.getResponseObject().getResponseObject();
            System.out.println("Method [" + candidate.getMethod().getName() + "] threw an exception");
            responseObject.printStackTrace(System.err);
            notifier.fireTestFailure(new Failure(testDescription, (Throwable)
                    verificationResultRaw.getResponseObject().getResponseObject()));

        }

        List<AtomicAssertion> assertionList = AtomicAssertionUtils.flattenAssertionMap(assertions);
        AssertionResult assertionResultMap = verificationResultRaw.getAssertionResult();

        if (verificationResultRaw.getResponseObject() == null) {
            System.out.println("Response is null");
            notifier.fireTestFailure(new Failure(testDescription,
                    new RuntimeException(String.valueOf(verificationResultRaw))));

        }

        AgentCommandRawResponse rawResponse = verificationResultRaw.getResponseObject();
        AgentCommandResponse acr = rawResponse.getAgentCommandResponse();
        Object responseObject = rawResponse.getResponseObject();
        if (responseObject instanceof Throwable) {
            System.out.println("Method threw an exception");
            ((Throwable) responseObject).printStackTrace();
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

            System.err.println("Expected [" + atomicAssertion.getExpectedValue() + "] instead of actual " +
                    "[" + expressedValue + "]\n\t when the return value from method " +
                    "[" + methodUnderTest.getName() + "()]\n\t value " +
                    "[" + (expression == Expression.SELF ? atomicAssertion.getKey() : (expression.name() +
                    "(" + atomicAssertion.getKey() + ")")) +
                    "] as expected in test candidate " +
                    "[" + candidate.getCandidateId() + "]" +
                    "[" + candidate.getName() + "]");
            RuntimeException thrownException = new RuntimeException(
                    "Expected [" + atomicAssertion.getExpectedValue() + "]instead of actual" +
                            "[" + expressedValue + "]\n\t when the return value from method " +
                            "[" + methodUnderTest.getName() + "]()\n\t value " +
                            "[" + (expression == Expression.SELF ? atomicAssertion.getKey() : (expression.name() +
                            "(" + atomicAssertion.getKey() + ")")) +
                            "] as expected in test candidate " +
                            "[" + candidate.getCandidateId() + "]" +
                            "[" + candidate.getName() + "]");
            Failure failure = new Failure(testDescription, thrownException);
            notifier.fireTestAssumptionFailed(failure);
        }


    }


    private AssertionResultWithRawObject executeAndVerify(StoredCandidate candidate, List<DeclaredMock> mockList) {

        List<String> methodArgumentValues = candidate.getMethodArguments();
        ArrayList<String> newArgumentValues = new ArrayList<>(methodArgumentValues.size());
        List<String> methodSignatureTypes = ClassTypeUtil.splitMethodDesc(candidate.getMethod().getSignature());
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
        return AssertionEngine.executeAssertions(
                candidate.getTestAssertions(),
                getResponseNode(String.valueOf(executionResult.getMethodReturnValue()),
                        executionResult.getResponseClassName()
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

    private static class DummyClosable implements Closeable {

        @Override
        public void close() throws IOException {

        }
    }

}