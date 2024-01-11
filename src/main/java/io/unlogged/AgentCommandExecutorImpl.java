package io.unlogged;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.unlogged.auth.RequestAuthentication;
import io.unlogged.auth.UnloggedSpringAuthentication;
import io.unlogged.command.*;
import io.unlogged.logging.IEventLogger;
import io.unlogged.mocking.*;
import io.unlogged.util.ClassTypeUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.jetbrains.annotations.NotNull;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class AgentCommandExecutorImpl implements AgentCommandExecutor {

    final private ObjectMapper objectMapper;
    final private IEventLogger logger;
    private final ByteBuddy byteBuddyInstance = new ByteBuddy()
            .with(new NamingStrategy.SuffixingRandom("Unlogged"));
    private final Map<String, MockInstance> globalFieldMockMap = new HashMap<>();
    Objenesis objenesis = new ObjenesisStd();
    private Object applicationContext;
    private Object springTestContextManager;
    private boolean isSpringPresent;
    private Method getBeanMethod;
    private Object springBeanFactory;

    public AgentCommandExecutorImpl(ObjectMapper objectMapper, IEventLogger logger) {
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    private static void closeHibernateSessionIfPossible(Object sessionInstance) {
        if (sessionInstance != null) {

            try {


                Method getTransactionMethod = sessionInstance.getClass().getMethod("getTransaction");
                Object transactionInstance = getTransactionMethod.invoke(sessionInstance);
//            System.err.println("Transaction to commit: " + transactionInstance);
                Method rollbackMethod = transactionInstance.getClass().getMethod("rollback");
                rollbackMethod.invoke(transactionInstance);


                Method sessionCloseMethod = sessionInstance.getClass().getMethod("close");
                sessionCloseMethod.invoke(sessionInstance);
            } catch (Exception e) {
                // failed to close session ?
            }
        }
    }

    public static Class<?>[] getAllInterfaces(Object o) {
        try {
            Set<Class<?>> results = new HashSet<>();
            getAllInterfaces(o, results::add);
            return results.toArray(new Class<?>[results.size()]);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    public static void getAllInterfaces(Object o, Function<Class<?>, Boolean> accumulator) throws IllegalArgumentException {
        if (null == o)
            return;

        if (null == accumulator)
            throw new IllegalArgumentException("Accumulator cannot be null");

        if (o instanceof Class) {
            Class clazz = (Class) o;

            if (clazz.isInterface()) {
                if (accumulator.apply((Class) o)) {
                    for (Class aClass : clazz.getInterfaces()) {
                        getAllInterfaces(aClass, accumulator);
                    }
                }
            } else {
                if (null != clazz.getSuperclass())
                    getAllInterfaces(clazz.getSuperclass(), accumulator);

                for (Class aClass : clazz.getInterfaces()) {
                    getAllInterfaces(aClass, accumulator);
                }
            }
        } else {
            getAllInterfaces(o.getClass(), accumulator);
        }
    }

    @Override
    public AgentCommandRawResponse executeCommandRaw(AgentCommandRequest agentCommandRequest) {
        if (agentCommandRequest == null) {
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setMessage("request is null");
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            return new AgentCommandRawResponse(agentCommandResponse, new Exception("request is null"));
        }
        AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
        AgentCommandRawResponse agentCommandRawResponse = new AgentCommandRawResponse(agentCommandResponse);
        AgentCommandRequestType requestType = agentCommandRequest.getRequestType();
        if (requestType == null) {
            requestType = AgentCommandRequestType.REPEAT_INVOKE;
        }
        try {
            if (requestType.equals(AgentCommandRequestType.REPEAT_INVOKE)) {
                logger.setRecording(true);
            }

            if (this.springTestContextManager == null) {
                this.springTestContextManager = logger.getObjectByClassName("org.springframework.boot.web" +
                        ".reactive.context" +
                        ".AnnotationConfigReactiveWebServerApplicationContext");
                setSpringApplicationContextAndLoadBeanFactory(this.springTestContextManager);
            }

            if (this.springTestContextManager == null) {
                this.springTestContextManager = logger.getObjectByClassName(
                        "org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext"
                );
                setSpringApplicationContextAndLoadBeanFactory(this.springTestContextManager);
            }

            Object sessionInstance = tryOpenHibernateSessionIfHibernateExists();
//            TestExecutionListener reactorContextTestExecutionListener = new ReactorContextTestExecutionListener();
//            reactorContextTestExecutionListener.beforeTestMethod(null);


            try {

                Class<?> targetClassType;
                ClassLoader targetClassLoader;

                Object objectInstanceByClass = null;

                String targetClassName = agentCommandRequest.getClassName();
                if (applicationContext != null && getBeanMethod != null) {
                    try {
                        objectInstanceByClass = getBeanMethod.invoke(applicationContext,
                                Class.forName(targetClassName));
                    } catch (Exception e) {

                    }
                }
                if (objectInstanceByClass == null) {
                    objectInstanceByClass = logger.getObjectByClassName(targetClassName);
                }

                List<String> alternateClassNames = agentCommandRequest.getAlternateClassNames();
                if (objectInstanceByClass == null && alternateClassNames != null && alternateClassNames.size() > 0) {
                    for (String alternateClassName : alternateClassNames) {
                        objectInstanceByClass = logger.getObjectByClassName(alternateClassName);
                        if (objectInstanceByClass != null) {
                            break;
                        }
                    }
                }
                ClassLoader targetClassLoader1 = logger.getTargetClassLoader();
                if (objectInstanceByClass == null) {
                    objectInstanceByClass = tryObjectConstruct(targetClassName, targetClassLoader1, new HashMap<>());
                }

				if (objectInstanceByClass != null) {
					targetClassType = objectInstanceByClass.getClass();
				}
				else {
					try {
						targetClassType = Class.forName(targetClassName, false, targetClassLoader1);
					}
					catch (Exception e) {
						agentCommandResponse.setTargetClassName(targetClassName);
						agentCommandResponse.setTargetMethodName(agentCommandRequest.getMethodName());
						agentCommandResponse.setTargetMethodSignature(agentCommandRequest.getMethodSignature());
						agentCommandResponse.setTimestamp(new Date().getTime());
						agentCommandResponse.setMethodReturnValue(null);
						agentCommandResponse.setResponseClassName(null);
						agentCommandResponse.setResponseType(ResponseType.FAILED);
						agentCommandResponse.setMessage(e.getMessage());
						
						agentCommandRawResponse.setResponseObject(null);
						return agentCommandRawResponse;
					}
				}
			
                targetClassLoader = objectInstanceByClass != null ?
                        objectInstanceByClass.getClass().getClassLoader() : targetClassLoader1;

                List<String> methodSignatureParts = ClassTypeUtil.splitMethodDesc(
                        agentCommandRequest.getMethodSignature());

                // DO NOT REMOVE this transformation
                String methodReturnType = methodSignatureParts.remove(methodSignatureParts.size() - 1);

                List<String> methodParameters = agentCommandRequest.getMethodParameters();

                Class<?>[] expectedMethodArgumentTypes = new Class[methodSignatureParts.size()];

                for (int i = 0; i < methodSignatureParts.size(); i++) {
                    String methodSignaturePart = methodSignatureParts.get(i);
//                System.err.println("Method parameter [" + i + "] type: " + methodSignaturePart);
                    expectedMethodArgumentTypes[i] =
                            ClassTypeUtil.getClassNameFromDescriptor(methodSignaturePart, targetClassLoader);
                }

                UnloggedSpringAuthentication authInstance = null;
                UnloggedSpringAuthentication usa = null;
                Object mockedContext = null;
                RequestAuthentication requestAuthentication = agentCommandRequest.getRequestAuthentication();
                if (requestAuthentication != null && requestAuthentication.getPrincipalClassName() != null) {

                    String principalString = String.valueOf(requestAuthentication.getPrincipal());
                    String userPrincipalClassName = requestAuthentication.getPrincipalClassName();

                    Object principalObject = objectMapper.readValue(principalString,
                            Class.forName(userPrincipalClassName));
                    requestAuthentication.setPrincipal(principalObject);

                    // spring is present
                    // so either have applicationContext or testApplicationContext
                    // instance of SpringApplicationContext


                    if (this.springTestContextManager != null) {
                        RequestAuthentication authRequest = requestAuthentication;

                        Class<?> authClass = Class.forName("org.springframework.security.core.Authentication");

                        Class<? extends UnloggedSpringAuthentication> springAuthImplementatorClass = byteBuddyInstance
                                .subclass(UnloggedSpringAuthentication.class)
                                .implement(authClass)
                                .make()
                                .load(targetClassLoader).getLoaded();

                        authInstance = springAuthImplementatorClass.getConstructor(
                                        RequestAuthentication.class)
                                .newInstance(authRequest);

                        mockedContext = Class.forName(
                                "org.springframework.security.core.context.SecurityContextImpl"
                        ).getConstructor(authClass).newInstance(authInstance);
                    }
                }


                // gets a method or throws exception if no such method
                Method methodToExecute = getMethodToExecute(targetClassType, agentCommandRequest.getMethodName(),
                        expectedMethodArgumentTypes);

                // we know more complex ways to do bypassing the security checks this thanks to lombok
                // but for now this will do
                methodToExecute.setAccessible(true);


                Class<?>[] parameterTypesClass = methodToExecute.getParameterTypes();

                Object[] parameters;
                try {
                    parameters = buildParametersUsingTargetClass(targetClassLoader, methodParameters,
                            parameterTypesClass, agentCommandRequest.getParameterTypes());
                } catch (InvalidDefinitionException ide1) {
                    if (!targetClassLoader.equals(targetClassLoader1)) {
                        parameters = buildParametersUsingTargetClass(targetClassLoader, methodParameters,
                                parameterTypesClass, agentCommandRequest.getParameterTypes());
                    } else {
                        throw ide1;
                    }
                }


                agentCommandResponse.setTargetClassName(targetClassName);
                agentCommandResponse.setTargetMethodName(agentCommandRequest.getMethodName());
                agentCommandResponse.setTargetMethodSignature(agentCommandRequest.getMethodSignature());
                agentCommandResponse.setTimestamp(new Date().getTime());

                List<DeclaredMock> declaredMocksList = agentCommandRequest.getDeclaredMocks();

                objectInstanceByClass = arrangeMocks(targetClassType, targetClassLoader, objectInstanceByClass,
                        declaredMocksList);

                Class<?> SECURITY_CONTEXT_KEY = null;
                try {
                    SECURITY_CONTEXT_KEY = Class.forName("org.springframework.security.core.context" +
                            ".SecurityContext");
                    isSpringPresent = true;
                } catch (Exception e) {
                    // no spring
                }

                Object methodReturnValue;
                if (isSpringPresent && mockedContext != null && springTestContextManager != null
                        && springTestContextManager.getClass()
                        .getCanonicalName().contains("AnnotationConfigReactiveWebServerApplicationContext")) {
                    final Map<String, Object> resultContainer = new HashMap<>();

                    final Mono<?> securityContext = Mono.just(mockedContext);
                    Object finalObjectInstanceByClass = objectInstanceByClass;
                    Object[] finalParameters = parameters;
                    assert SECURITY_CONTEXT_KEY != null;

                    Class<?> finalSECURITY_CONTEXT_KEY = SECURITY_CONTEXT_KEY;

                    CountDownLatch cdl = new CountDownLatch(1);
                    Mono.defer(() -> {
                                try {
                                    // Invoke the method using reflection
                                    Object returnValue = methodToExecute.invoke(finalObjectInstanceByClass, finalParameters);

                                    // Handle different return types
                                    if (returnValue instanceof Mono) {
                                        return (Mono<?>) returnValue;
                                    } else if (returnValue instanceof Flux) {
                                        return ((Flux<?>) returnValue).collectList();
                                    } else {
                                        return Mono.justOrEmpty(returnValue);
                                    }
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    return Mono.error(e);
                                }
                            })
                            .contextWrite(ctx -> {
//                                System.out.println("setting sec");
                                return ctx.put(finalSECURITY_CONTEXT_KEY, securityContext);
                            }) // Set the security context
                            .doOnSuccess(result -> {
                                resultContainer.put("returnValue", result);
                                cdl.countDown(); // Count down on successful completion
                            })
                            .doOnError(error -> {
                                resultContainer.put("exception", error);
                                cdl.countDown(); // Count down on error
                            })
                            .subscribe();

                    cdl.await();


                    if (resultContainer.containsKey("returnValue")) {
                        methodReturnValue = resultContainer.get("returnValue");
                    } else {
                        throw (Throwable) resultContainer.get("exception");
                    }

                } else {

                    try {
                        Class<?> sch = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                        Method gcm = sch.getMethod("getContext");
                        Object contextInstance = gcm.invoke(null);
                        Class<?> authenticationClass = Class.forName(
                                "org.springframework.security.core.Authentication");
                        Method setMethod = contextInstance.getClass()
                                .getMethod("setAuthentication", authenticationClass);
                        setMethod.invoke(contextInstance, authInstance);

                    } catch (Exception e) {
                        // failed to set auth for non reactive spring app
                    }

                    methodReturnValue = methodToExecute.invoke(objectInstanceByClass, parameters);
                }
//                reactorContextTestExecutionListener.afterTestMethod(null);

                Object serializedValue = serializeMethodReturnValue(methodReturnValue);
                agentCommandResponse.setMethodReturnValue(serializedValue);

                agentCommandResponse.setResponseClassName(methodToExecute.getReturnType().getCanonicalName());
                agentCommandResponse.setResponseType(ResponseType.NORMAL);
                agentCommandRawResponse.setResponseObject(methodReturnValue);

                return agentCommandRawResponse;
            } catch (Throwable exception) {
                if (exception instanceof InvocationTargetException) {
                    agentCommandResponse.setResponseType(ResponseType.EXCEPTION);
                    exception.getCause().printStackTrace();
                } else {
                    agentCommandResponse.setResponseType(ResponseType.FAILED);
//                    exception.printStackTrace();
                }
                Throwable exceptionCause = exception.getCause() != null ? exception.getCause() : exception;
                agentCommandResponse.setMessage(exceptionCause.getMessage());
                agentCommandRawResponse.setResponseObject(exceptionCause);
                try {
                    agentCommandResponse.setMethodReturnValue(objectMapper.writeValueAsString(exceptionCause));
                } catch (Throwable e) {
                    agentCommandResponse.setMethodReturnValue("Exception: " + exceptionCause.getMessage());
                    agentCommandResponse.setMessage("Exception: " + exceptionCause.getMessage());
                    // failed to serialize thrown exception
                }
                agentCommandResponse.setResponseClassName(exceptionCause.getClass().getCanonicalName());
            } finally {
                closeHibernateSessionIfPossible(sessionInstance);
            }
        } catch (Throwable exception) {
//            exception.printStackTrace();
            Throwable exceptionCause = exception.getCause() != null ? exception.getCause() : exception;
            agentCommandResponse.setMessage(exceptionCause.getMessage());
            try {
                agentCommandRawResponse.setResponseObject(exceptionCause);
                agentCommandResponse.setMethodReturnValue(objectMapper.writeValueAsString(exceptionCause));
            } catch (Throwable e) {
                agentCommandResponse.setMethodReturnValue("Exception: " + exceptionCause.getMessage());
                agentCommandResponse.setMessage("Exception: " + exceptionCause.getMessage());
                // failed to serialize thrown exception
            }
            agentCommandResponse.setResponseClassName(exceptionCause.getClass().getCanonicalName());
            agentCommandResponse.setResponseType(ResponseType.FAILED);
        } finally {
            if (requestType.equals(AgentCommandRequestType.REPEAT_INVOKE)) {
                logger.setRecording(false);
            }
        }
        return agentCommandRawResponse;

    }

    @NotNull
    private AnnotationDescription getAnnotationDescription(String className) throws ClassNotFoundException {
        Class<?> springBootTestAnnotationClass = Class.forName(className);


        AnnotationDescription springBootTestAnnotation =
                AnnotationDescription.Builder
                        .ofType((Class<? extends Annotation>) springBootTestAnnotationClass)
                        .build();
        return springBootTestAnnotation;
    }

    @Override
    public AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) {
        AgentCommandRawResponse rawResponse = executeCommandRaw(agentCommandRequest);
        return rawResponse.getAgentCommandResponse();
    }

    @Override
    public AgentCommandResponse injectMocks(AgentCommandRequest agentCommandRequest) {

        int fieldCount = 0;
        int classCount = 0;
        Map<String, List<DeclaredMock>> mocksBySourceClass = agentCommandRequest
                .getDeclaredMocks().stream().collect(Collectors.groupingBy(DeclaredMock::getSourceClassName));


        for (String sourceClassName : mocksBySourceClass.keySet()) {
            Object sourceClassInstance = logger.getObjectByClassName(sourceClassName);
            if (sourceClassInstance == null) {
                // no instance found for this class
                // nothing to inject mocks to
                continue;
            }
            List<DeclaredMock> declaredMocks = mocksBySourceClass.get(sourceClassName);

            Map<String, List<DeclaredMock>> mocksByField = declaredMocks.stream()
                    .collect(Collectors.groupingBy(DeclaredMock::getFieldName));

            Class<? extends Object> classObject = sourceClassInstance.getClass();
            ClassLoader targetClassLoader = classObject.getClassLoader();
            String targetClassName = classObject.getCanonicalName();

            ClassLoadingStrategy<ClassLoader> strategy;


            while (classObject != Object.class) {
                classCount++;
                Field[] availableFields = classObject.getDeclaredFields();

                for (Field field : availableFields) {
                    String fieldName = field.getName();
                    if (!mocksByField.containsKey(fieldName)) {
                        continue;
                    }
                    List<DeclaredMock> declaredMocksForField = mocksByField.get(fieldName);

                    String key = sourceClassName + "#" + fieldName;
                    MockInstance existingMockInstance = globalFieldMockMap.get(key);

                    field.setAccessible(true);
                    Object originalFieldValue = null;
                    try {
                        originalFieldValue = field.get(sourceClassInstance);
                    } catch (IllegalAccessException e) {
                        // if it does happen, skip mocking of this field for now
                        System.err.println("Failed to access field [" + targetClassName + "#" + fieldName + "] " +
                                "=> " + e.getMessage());
                        continue;
                    }


                    if (existingMockInstance == null) {
                        MockHandler mockHandler = new MockHandler(declaredMocksForField, objectMapper,
                                byteBuddyInstance, objenesis, originalFieldValue, sourceClassInstance,
                                targetClassLoader, field);
                        Class<?> fieldType = field.getType();

                        DynamicType.Loaded<?> loadedMockedField;
                        loadedMockedField = createInstanceUsingByteBuddy(targetClassLoader, mockHandler, fieldType);


                        Object mockedFieldInstance = objenesis.newInstance(loadedMockedField.getLoaded());

                        existingMockInstance = new MockInstance(mockedFieldInstance, mockHandler, loadedMockedField);
                        globalFieldMockMap.put(key, existingMockInstance);

                    } else {
                        existingMockInstance.getMockHandler().setDeclaredMocks(declaredMocksForField);
                    }

                    try {
                        field.set(sourceClassInstance, existingMockInstance.getMockedFieldInstance());
                    } catch (IllegalAccessException e) {
                        System.err.println("Failed to mock field [" + sourceClassName + "#" + fieldName + "] =>" +
                                e.getMessage());
                    }
                    fieldCount++;

                }


                classObject = classObject.getSuperclass();
            }


        }

        AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
        agentCommandResponse.setResponseType(ResponseType.NORMAL);
        agentCommandResponse.setMessage("Mocks injected for [" +
                fieldCount + "] fields in [" + classCount + "] classes");
        return agentCommandResponse;
    }


    private DynamicType.Loaded<?> createInstanceUsingByteBuddy(ClassLoader targetClassLoader, MockHandler mockHandler, Class<?> classType) {
        ClassLoadingStrategy.Default strategy = ClassLoadingStrategy.Default.INJECTION;
        DynamicType.Loaded<?> loadedMockedField;
        if (classType.isInterface()) {
            Class<?>[] implementedInterfaces = getAllInterfaces(classType);
            Set<String> implementedClasses = new HashSet<>();
            implementedClasses.add(classType.getCanonicalName());

            List<Class<?>> pendingImplementations = new ArrayList<>();
            pendingImplementations.add(classType);
            for (Class<?> implementedInterface : implementedInterfaces) {
                if (implementedClasses.contains(implementedInterface.getCanonicalName())) {
                    continue;
                }
                implementedClasses.add(implementedInterface.getCanonicalName());
                pendingImplementations.add(implementedInterface);
            }

            loadedMockedField = byteBuddyInstance
                    .subclass(Object.class)
                    .name(classType.getCanonicalName() + "$UnloggedFakeInterfaceImpl")
                    .implement(pendingImplementations)
                    .intercept(MethodDelegation.to(mockHandler))
                    .make()
                    .load(targetClassLoader, strategy);

        } else {
            loadedMockedField = byteBuddyInstance
                    .subclass(classType)
                    .name(classType.getCanonicalName() + "$UnloggedFakeImpl")
                    .method(isDeclaredBy(classType))
                    .intercept(MethodDelegation.to(mockHandler))
                    .make()
                    .load(targetClassLoader, strategy);
        }
        return loadedMockedField;
    }

    @Override
    public AgentCommandResponse removeMocks(AgentCommandRequest agentCommandRequest) throws Exception {

        int fieldCount = 0;
        int classCount = 0;
        Set<String> strings = new HashSet<>(globalFieldMockMap.keySet());
        Set<String> classNames = new HashSet<>();
        List<DeclaredMock> mocksToDelete = agentCommandRequest.getDeclaredMocks();
        Map<String, List<DeclaredMock>> mocksByClassName = mocksToDelete == null ? new HashMap<>() :
                mocksToDelete.stream().collect(Collectors.groupingBy(DeclaredMock::getSourceClassName));
        if (mocksByClassName.size() > 0) {
            // remove some mocks
            for (String sourceClassName : mocksByClassName.keySet()) {
                Map<String, List<DeclaredMock>> classMocksByFieldName = mocksByClassName
                        .get(sourceClassName).stream()
                        .collect(Collectors.groupingBy(DeclaredMock::getFieldName));
                for (String fieldName : classMocksByFieldName.keySet()) {
                    String key = sourceClassName + "#" + fieldName;
                    MockInstance mockInstance = globalFieldMockMap.get(key);
                    if (mockInstance == null) {
                        continue;
                    }
                    if (!classNames.contains(sourceClassName)) {
                        classNames.add(sourceClassName);
                        classCount++;
                    }
                    fieldCount++;
                    MockHandler mockHandler = mockInstance.getMockHandler();
                    mockHandler.removeDeclaredMock(classMocksByFieldName.get(fieldName));
                }

            }

        } else {
            // remove all mocks
            for (String key : strings) {
                fieldCount++;
                MockInstance mockInstance = globalFieldMockMap.get(key);
                MockHandler mockHandler = mockInstance.getMockHandler();
                Object parentInstance = mockHandler.getOriginalFieldParent();
                String sourceObjectTypeName = parentInstance.getClass().getCanonicalName();
                if (!classNames.contains(sourceObjectTypeName)) {
                    classNames.add(sourceObjectTypeName);
                    classCount++;
                }
                Object originalFieldInstance = mockHandler.getOriginalImplementation();
                Field field = mockHandler.getField();
                field.setAccessible(true);
                field.set(parentInstance, originalFieldInstance);
                globalFieldMockMap.remove(key);
            }

        }

        AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
        agentCommandResponse.setResponseType(ResponseType.NORMAL);
        agentCommandResponse.setMessage("Mocks removed for [" +
                fieldCount + "] fields in [" + classCount + "] classes");
        return agentCommandResponse;

    }

    public Object arrangeMocks(Class<?> targetClassType, ClassLoader targetClassLoader,
                               Object objectInstanceByClass, List<DeclaredMock> declaredMocksList) {
        if (declaredMocksList == null || declaredMocksList.size() == 0) {
            return objectInstanceByClass;
        }

        Map<String, MockInstance> mockInstanceMap = new HashMap<>();

        String targetClassName = targetClassType.getCanonicalName();
        Map<String, List<DeclaredMock>> mocksByFieldName = declaredMocksList.stream()
                .collect(Collectors.groupingBy(DeclaredMock::getFieldName,
                        Collectors.toList()));


        Object extendedClassInstance;
        if (Modifier.isFinal(targetClassType.getModifiers())) {
            extendedClassInstance = objectInstanceByClass;
        } else {
            DynamicType.Unloaded<? extends Object> extendedClass = byteBuddyInstance
                    .subclass(targetClassType)
                    .make();

            ClassLoadingStrategy<ClassLoader> strategy;
            if (ClassInjector.UsingLookup.isAvailable()) {
                Class<?> methodHandles = null;
                try {
                    methodHandles = targetClassLoader.loadClass("java.lang.invoke.MethodHandles");
                    Object lookup = methodHandles.getMethod("lookup").invoke(null);
                    Method privateLookupIn = methodHandles.getMethod("privateLookupIn",
                            Class.class,
                            targetClassLoader.loadClass("java.lang.invoke.MethodHandles$Lookup"));
                    Object privateLookup = privateLookupIn.invoke(null, targetClassType, lookup);
                    strategy = ClassLoadingStrategy.UsingLookup.of(privateLookup);
                } catch (Exception e) {
                    // should never happen
                    throw new RuntimeException(e);
                }
            } else if (ClassInjector.UsingReflection.isAvailable()) {
                strategy = ClassLoadingStrategy.Default.INJECTION;
            } else {
                throw new IllegalStateException("No code generation strategy available");
            }

            DynamicType.Loaded<? extends Object> extendedClassLoaded = extendedClass.load(targetClassLoader, strategy);

            extendedClassInstance = objenesis.newInstance(extendedClassLoaded.getLoaded());
        }


        Class<?> fieldCopyForClass = targetClassType;
        while (fieldCopyForClass != null && !fieldCopyForClass.equals(Object.class)) {
            Field[] declaredFields = fieldCopyForClass.getDeclaredFields();

            for (Field field : declaredFields) {
                try {

                    field.setAccessible(true);
                    List<DeclaredMock> declaredMocksForField = mocksByFieldName.get(field.getName());

                    Object fieldValue = field.get(objectInstanceByClass);
                    boolean fieldTypeIsFinal = Modifier.isFinal(field.getType().getModifiers());
                    if (declaredMocksForField == null || declaredMocksForField.size() == 0 || fieldTypeIsFinal) {
                        if (fieldValue == null) {
                            try {
                                fieldValue = objenesis.newInstance(field.getType());
                            } catch (Throwable e) {
                                continue;
                            }
                        }
//                        if (Modifier.isFinal(field.getModifiers())) {
//                            continue;
//                        }
                        try {
                            field.set(extendedClassInstance, fieldValue);
                        } catch (Throwable e) {
                            //
                        }
                    } else {

                        String key = targetClassName + "#" + field.getName();
                        MockInstance existingMockInstance = mockInstanceMap.get(key);


                        if (existingMockInstance == null) {

                            if (objectInstanceByClass == null) {
                                System.err.println("original instance is null [" + field.getType()
                                        .getCanonicalName() + " " + field.getName());
                            }

                            if (fieldValue == null) {
								try {
									fieldValue = createObjectInstanceFromStringAndTypeInformation(
                                        field.getType().getCanonicalName(), objectMapper.getTypeFactory(), "{}", field.getType()
                                		);
								}
                                catch (Exception e) {
									fieldValue = null;
								}
                            }
                            String fieldTypeName = declaredMocksForField.get(0).getFieldTypeName();
                            Class<?> classTypeToBeMocked = Class.forName(fieldTypeName);
                            existingMockInstance = createMockedInstance(targetClassLoader, objectInstanceByClass,
                                    field, declaredMocksForField, fieldValue, classTypeToBeMocked);
                            mockInstanceMap.put(key, existingMockInstance);

                        } else {
                            existingMockInstance.getMockHandler().setDeclaredMocks(declaredMocksForField);
                        }

//                        System.out.println("Setting mocked field [" + field.getName() + "] => " + existingMockInstance.getMockedFieldInstance());
                        field.set(extendedClassInstance, existingMockInstance.getMockedFieldInstance());
                    }


                } catch (Exception e) {
                    if (e.getMessage().startsWith("Can not set static final")) {
                        // not printing this
                    } else {
                        e.printStackTrace();
                        System.err.println(
                                "Failed to set value for field [" + field.getName() + "] => " + e.getMessage());
                    }
                }
            }

            fieldCopyForClass = fieldCopyForClass.getSuperclass();
        }
        return extendedClassInstance;
    }


    private MockInstance createMockedInstance(
            ClassLoader targetClassLoader,
            Object objectInstanceByClass, Field field,
            List<DeclaredMock> declaredMocksForField,
            Object fieldValue, Class<?> fieldType) {
        ClassLoadingStrategy<ClassLoader> strategy = ClassLoadingStrategy.Default.INJECTION;
        MockInstance existingMockInstance;
        if (objectInstanceByClass == null) {
            if (field != null) {
                System.out.println(
                        "objectInstanceByClass is null: " + field.getType().getCanonicalName() + " " + field.getName());
            } else {
            }
        }
        MockHandler mockHandler = new MockHandler(declaredMocksForField, objectMapper,
                byteBuddyInstance, objenesis, fieldValue, objectInstanceByClass, targetClassLoader, field);

        DynamicType.Loaded<?> loadedMockedField;
        if (fieldType.isInterface()) {
            Class<?>[] implementedInterfaces = getAllInterfaces(fieldType);
            Set<String> implementedClasses = new HashSet<>();
            implementedClasses.add(fieldType.getCanonicalName());

            List<Class<?>> pendingImplementations = new ArrayList<>();
            pendingImplementations.add(fieldType);
            for (Class<?> implementedInterface : implementedInterfaces) {
                if (implementedClasses.contains(implementedInterface.getCanonicalName())) {
                    continue;
                }
                implementedClasses.add(implementedInterface.getCanonicalName());
                pendingImplementations.add(implementedInterface);
            }

            loadedMockedField = byteBuddyInstance
                    .subclass(Object.class)
                    .implement(pendingImplementations)
                    .intercept(MethodDelegation.to(mockHandler))
                    .make()
                    .load(targetClassLoader, strategy);

        } else {
            loadedMockedField = byteBuddyInstance
                    .subclass(fieldType)
                    .method(isDeclaredBy(fieldType)).intercept(MethodDelegation.to(mockHandler))
                    .make()
                    .load(targetClassLoader, strategy);
        }


        Object mockedFieldInstance = objenesis.newInstance(loadedMockedField.getLoaded());

        existingMockInstance = new MockInstance(mockedFieldInstance, mockHandler, loadedMockedField);
        return existingMockInstance;
    }

    private Method getMethodToExecute(Class<?> objectClass, String expectedMethodName,
                                      Class<?>[] expectedMethodArgumentTypes)
            throws NoSuchMethodException {

        StringBuilder className = new StringBuilder();

        Method methodToExecute = null;
        List<String> methodNamesList = new ArrayList<>();
        while (objectClass != null && !objectClass.equals(Object.class)) {

            className.append(objectClass.getCanonicalName()).append(", ");
            try {
                methodToExecute = objectClass
                        .getDeclaredMethod(expectedMethodName, expectedMethodArgumentTypes);
            } catch (NoSuchMethodException ignored) {

            }

            try {
                methodToExecute = objectClass
                        .getMethod(expectedMethodName, expectedMethodArgumentTypes);
            } catch (NoSuchMethodException ignored) {

            }

            if (methodToExecute == null) {
                Method[] methods = objectClass.getDeclaredMethods();
                for (Method method : methods) {
                    String methodName = method.getName();
                    methodNamesList.add(methodName);
                    if (methodName.equals(expectedMethodName)
                            && method.getParameterCount() == expectedMethodArgumentTypes.length) {

                        Class<?>[] actualParameterTypes = method.getParameterTypes();

                        boolean match = true;
                        for (int i = 0; i < expectedMethodArgumentTypes.length; i++) {
                            Class<?> methodParameterType = expectedMethodArgumentTypes[i];
                            Class<?> actualParamType = actualParameterTypes[i];
                            if (!actualParamType.getCanonicalName()
                                    .equals(methodParameterType.getCanonicalName())) {
                                match = false;
                                break;
                            }
                        }

                        if (match) {
                            methodToExecute = method;
                            break;
                        }

                    }
                }
            }
            if (methodToExecute != null) {
                break;
            }
            objectClass = objectClass.getSuperclass();
        }
        if (methodToExecute == null) {
            System.err.println("Method not found: " + expectedMethodName
                    + ", methods were: " + methodNamesList);
            throw new NoSuchMethodException("method not found [" + expectedMethodName
                    + "] in class [" + className + "]. Available methods are: "
                    + methodNamesList);
        }

        return methodToExecute;
    }

    private Object serializeMethodReturnValue(Object methodReturnValue) throws JsonProcessingException {
        if (methodReturnValue == null) {
            return null;
        }
        if (methodReturnValue instanceof Double) {
            return Double.doubleToLongBits((Double) methodReturnValue);
        } else if (methodReturnValue instanceof Float) {
            return Float.floatToIntBits((Float) methodReturnValue);
//                    } else if (methodReturnValue instanceof Flux) {
//                        Flux<?> returnedFlux = (Flux<?>) methodReturnValue;
//                        agentCommandResponse.setMethodReturnValue(
//                                objectMapper.writeValueAsString(returnedFlux.collectList().block()));
//                    } else if (methodReturnValue instanceof Mono) {
//                        Mono<?> returnedFlux = (Mono<?>) methodReturnValue;
//                        agentCommandResponse.setMethodReturnValue(
//                                objectMapper.writeValueAsString(returnedFlux.block()));
        } else if (methodReturnValue instanceof String) {
            return methodReturnValue;
        } else {
            try {
                if (methodReturnValue instanceof Flux) {
                    Flux<?> returnedFlux = (Flux<?>) methodReturnValue;
                    return objectMapper.writeValueAsString(returnedFlux.collectList().block());
                } else if (methodReturnValue instanceof Mono) {
                    Mono<?> returnedFlux = (Mono<?>) methodReturnValue;
                    return objectMapper.writeValueAsString(returnedFlux.block());
                }
                return objectMapper.writeValueAsString(methodReturnValue);
            } catch (Exception ide) {
                return "Failed to serialize response object of " +
                        "type: " + (methodReturnValue.getClass() != null ?
                        methodReturnValue.getClass().getCanonicalName() : methodReturnValue);
            }
        }
    }

    private Object[] buildParametersUsingTargetClass(
            ClassLoader targetClassLoader,
            List<String> methodParameters,
            Class<?>[] parameterTypesClass,
            List<String> parameterTypes) throws JsonProcessingException {
        TypeFactory typeFactory = objectMapper.getTypeFactory().withClassLoader(targetClassLoader);
        Object[] parameters = new Object[methodParameters.size()];

        for (int i = 0; i < methodParameters.size(); i++) {
            String methodParameterStringValue = methodParameters.get(i);
            Class<?> parameterType = parameterTypesClass[i];
            String parameterTypeName = parameterTypes.get(i); 
			Object parameterObject;
			try {
				parameterObject = createObjectInstanceFromStringAndTypeInformation(parameterTypeName, typeFactory,
                    methodParameterStringValue, parameterType);
			}
            catch (Exception e) {
				parameterObject = null;
			}

            parameters[i] = parameterObject;
        }
        return parameters;
    }


    private Object createObjectInstanceFromStringAndTypeInformation
            (String stringParameterType, TypeFactory typeFactory, String methodParameter, Class<?> parameterType) throws JsonProcessingException {
        Object parameterObject = null;
        if (parameterType.getCanonicalName().equals("org.springframework.util.MultiValueMap")) {
            try {
                parameterObject = objectMapper.readValue(methodParameter,
                        Class.forName("org.springframework.util.LinkedMultiValueMap"));
            } catch (ClassNotFoundException e) {
                // this should never happen
            }
        } else {

            JavaType typeReference = null;
            try {
                if (stringParameterType != null) {
                    typeReference = MockHandler.getTypeReference(typeFactory, stringParameterType);
                }
            } catch (Throwable e) {
                // failed to construct from the canonical name,
                // happens when this is a generic type
                // so we try to construct using type from the method param class

            }
            if (typeReference == null) {
                typeReference = typeFactory.constructType(parameterType);
            }
            try {
                parameterObject = objectMapper.readValue(methodParameter, typeReference);
            } catch (Throwable e) {
                // a complicated type (no default args constructor), or interface which jackson cannot create ?
                try {
                    // can we try using objenesis ?
                    parameterObject = createParameterUsingObjenesis(typeReference, methodParameter, typeFactory);
                    // we might want to now construct the whole object tree deep down
                } catch (Throwable e1) {
                    // constructing using objenesis also failed
                    // lets try extending or implementing the class ?
                    parameterObject = createParameterUsingMocking(methodParameter, parameterType);
                }
            }
        }
        return parameterObject;
    }

    private Object createParameterUsingObjenesis(JavaType typeReference, String methodParameter, TypeFactory typeFactory)
            throws JsonProcessingException, IllegalAccessException {
        Class<?> rawClass = typeReference.getRawClass();
        Object parameterObject = objenesis.newInstance(rawClass);
        Class<?> currentClass = rawClass;
        JsonNode providedValues = objectMapper.readTree(methodParameter);
        while (!currentClass.equals(Object.class)) {
            Field[] declaredFields = rawClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                JsonNode fieldValueInNodeByName = providedValues.get(declaredField.getName());
                Object valueToSet = getValueToSet(typeFactory, fieldValueInNodeByName, declaredField.getType());
                declaredField.setAccessible(true);
                declaredField.set(parameterObject, valueToSet);
            }
            currentClass = currentClass.getSuperclass();
        }
        return parameterObject;
    }


    private Object createParameterUsingMocking(String methodParameter, Class<?> parameterType) throws JsonProcessingException {
        Class<?> currentClass;
        List<DeclaredMock> mockList = new ArrayList<>();

        JsonNode providedValues = objectMapper.readTree(methodParameter);
        currentClass = parameterType;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            Method[] definedMethods = currentClass.getDeclaredMethods();
            for (Method definedMethod : definedMethods) {
                String methodName = definedMethod.getName();
                String potentialFieldName = null;
                Class<?> valueType = null;
                if (methodName.startsWith("get") && definedMethod.getParameterTypes().length == 0) {
                    potentialFieldName = methodName.substring(3);
                    valueType = definedMethod.getReturnType();
//                            } else if (methodName.startsWith("set") && definedMethod.getParameterTypes().length == 1) {
//                                valueType = definedMethod.getParameterTypes()[0];
//                                potentialFieldName = methodName.substring(3);
                } else if (methodName.startsWith("is") && definedMethod.getParameterTypes().length == 0) {
                    valueType = definedMethod.getReturnType();
                    potentialFieldName = methodName.substring(2);
                }

                if (potentialFieldName == null) {
                    potentialFieldName = "5";
                }

                potentialFieldName = potentialFieldName.substring(0, 1).toLowerCase() + potentialFieldName.substring(1);

                if (providedValues.has(potentialFieldName)) {
                    JsonNode providedValue = providedValues.get(potentialFieldName);
//                                Object valueToReturn = getValueToSet(typeFactory, providedValue, valueType);

                    if (methodName.startsWith("get") || methodName.startsWith("is")) {
                        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();

                        ReturnValue returnParameter;
                        if (checkCanClassBeExtended(valueType)) {
                            returnParameter = new ReturnValue(
                                    providedValue.toString(), valueType.getCanonicalName(), ReturnValueType.MOCK);
                        } else {
                            returnParameter = new ReturnValue(
                                    providedValue.toString(), valueType.getCanonicalName(), ReturnValueType.REAL);
                        }

                        DeclaredMock returnParamCallMock = new DeclaredMock();
                        returnParameter.addDeclaredMock(returnParamCallMock);

                        ThenParameter thenParameter = new ThenParameter(returnParameter, MethodExitType.NORMAL);

                        thenParameterList.add(thenParameter);
                        DeclaredMock dummyMockDefinition = new DeclaredMock(
                                "generated mock for " + methodName, valueType.getCanonicalName(),
                                potentialFieldName, methodName, new ArrayList<>(), thenParameterList
                        );
                        mockList.add(dummyMockDefinition);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        MockInstance parameterObject;
        try {
            parameterObject = createMockedInstance(parameterType.getClassLoader(), null,
                    null, mockList, null, parameterType);
        } catch (Exception e) {
            // cant say that
            // just failed to make an instance of this
            // log something ?
            return null;
        }
        return parameterObject.getMockedFieldInstance();
    }

    private boolean checkCanClassBeExtended(Class<?> fieldType) {
        if (fieldType.isPrimitive()) {
            return false;
        }
        if (fieldType.isArray()) {
            return false;
        }
        if ((fieldType.getModifiers() & java.lang.reflect.Modifier.FINAL) != 0) {
            return false;
        }

        return true;
    }

    private Object getValueToSet(TypeFactory typeFactory, JsonNode fieldValueInNodeByName, Class<?> type) throws JsonProcessingException {
        Object valueToSet = null;
        if (int.class.equals(type) || Integer.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.intValue();
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.longValue();
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.doubleValue();
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.floatValue();
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.booleanValue();
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.shortValue();
        } else if (String.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.textValue();
        } else if (StringBuilder.class.equals(type)) {
            valueToSet = new StringBuilder(fieldValueInNodeByName.textValue());
        } else {
            valueToSet = createObjectInstanceFromStringAndTypeInformation(
                    null, typeFactory, fieldValueInNodeByName.asText(), type);
        }
        return valueToSet;
    }


    private Object tryObjectConstruct(String className, ClassLoader targetClassLoader, Map<String, Object> buildMap)
            throws InstantiationException, IllegalAccessException {
        if (className.equals("java.util.List")) {
            return new ArrayList<>();
        }
        if (className.equals("java.util.Map")) {
            return new HashMap<>();
        }
        Object newInstance = null;
        if (targetClassLoader == null) {
            System.err.println("Failed to construct instance of class [" + className + "]. classLoader is not defined");
        }
        Class<?> loadedClass;
        try {
            loadedClass = targetClassLoader.loadClass(className);
        } catch (ClassNotFoundException classNotFoundException) {
            // class not found
            // or this is an internal class ? try to check one level up class ?
            if (className.lastIndexOf(".") == -1) {
                // todo: if it was an array of an internal class
                // com.something.package.ParentClass$ChildClass[][]
                return null;
            }
            String parentName = className.substring(0, className.lastIndexOf("."));
            try {
                Class<?> parentClassType = targetClassLoader.loadClass(parentName);
                // if we found this, then
                loadedClass =
                        targetClassLoader.loadClass(
                                parentName + "$" + className.substring(className.lastIndexOf(".") + 1));

            } catch (ClassNotFoundException cne) {
                // try another level ? just to be sure ?better way to identify internal classes ?
                return null;
            }
        }
        Constructor<?> noArgsConstructor = null;
        try {
            noArgsConstructor = loadedClass.getConstructor();
            try {
                newInstance = noArgsConstructor.newInstance();
            } catch (InvocationTargetException | InstantiationException e) {
//                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
            //
        }
        if (newInstance == null) {
            Method[] methods = loadedClass.getMethods();
            // try to get the instance of the class using Singleton.getInstance
            for (Method method : methods) {
                if (method.getParameterCount() == 0 && Modifier.isStatic(method.getModifiers())) {
                    if (method.getReturnType().equals(loadedClass)) {
                        try {
                            return method.invoke(null);
                        } catch (InvocationTargetException ex) {
                            // this method for potentially getting instance from static getInstance type method
                            // did not work
                        }
                    }
                }
            }
        }
        if (newInstance == null) {
            try {
                newInstance = objenesis.newInstance(loadedClass);
            } catch (java.lang.InstantiationError | IllegalAccessError e) {
                // failed to create using objenesis
            }
        }

        if (newInstance == null) {
            try {
                MockHandler mockHandler = new MockHandler(new ArrayList<>(), objectMapper, byteBuddyInstance,
                        objenesis, null, null, targetClassLoader, null);
                DynamicType.Loaded<?> newInstanceLoader = createInstanceUsingByteBuddy(targetClassLoader, mockHandler,
                        loadedClass);
                newInstance = objenesis.newInstance(newInstanceLoader.getLoaded());

            } catch (Exception exception) {
                // failed to create using bytebuddy also
                //
            }
        }

        buildMap.put(className, newInstance);
        if (newInstance == null) {
            return newInstance;
        }

        // field injections
        Class<?> fieldsForClass = loadedClass;

        while (fieldsForClass != null && !fieldsForClass.equals(Object.class)) {
            Field[] fields = fieldsForClass.getDeclaredFields();


            for (Field field : fields) {

                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    continue;
                }

                String fieldTypeName = field.getType().getCanonicalName();
                Object value = field.get(newInstance);
                if (value != null) {
                    continue;
                }
                if (buildMap.containsKey(fieldTypeName)) {
                    value = buildMap.get(fieldTypeName);
                } else {
                    value = tryObjectConstruct(fieldTypeName, targetClassLoader, buildMap);
                    if (value == null) {
                        continue;
                    }
                    buildMap.put(fieldTypeName, value);
                }
                try {
                    field.set(newInstance, value);
                } catch (Throwable th) {
                    th.printStackTrace();
                    System.out.println("Failed to set field value: " + th.getMessage());
                }

            }


            fieldsForClass = fieldsForClass.getSuperclass();
        }

        return newInstance;

    }


    private Object trySpringTransaction() {
        try {
            Class<?> transactionManagerClass = Class.forName("org.springframework.transaction" +
                    ".TransactionManager");
            Object transactionManager = getBeanMethod.invoke(applicationContext, transactionManagerClass);
            transactionManagerClass.getMethod("getTransaction");
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Object tryOpenHibernateSessionIfHibernateExists() {
        Object hibernateSessionFactory = logger.getObjectByClassName("org.hibernate.internal.SessionFactoryImpl");
        if (hibernateSessionFactory == null && getBeanMethod != null && applicationContext != null) {
            try {
                hibernateSessionFactory = getBeanMethod.invoke(applicationContext,
                        Class.forName("org.hibernate.SessionFactory"));
            } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                // hibernate not present on class path
                return null;
            }
        }
        Object sessionInstance = null;
        if (hibernateSessionFactory != null) {
            try {


//            System.err.println("Hibernate session factory: " + hibernateSessionFactory);
                Method openSessionMethod = hibernateSessionFactory.getClass().getMethod("openSession");
                sessionInstance = openSessionMethod.invoke(hibernateSessionFactory);
//            System.err.println("Hibernate session opened: " + sessionInstance);
                Class<?> managedSessionContextClass = Class.forName(
                        "org.hibernate.context.internal.ManagedSessionContext");
                Method bindMethod = managedSessionContextClass.getMethod("bind",
                        Class.forName("org.hibernate.Session"));
                bindMethod.invoke(null, sessionInstance);


                Method beginTransactionMethod = sessionInstance.getClass().getMethod("beginTransaction");
                beginTransactionMethod.invoke(sessionInstance);

            } catch (Exception e) {
                // failed to create hibernate session
                return null;
            }

        }
        return sessionInstance;
    }

    private void trySpringIntegration(Class<?> testClass) {
        // spring loader
        // if spring exists
        try {
            Class.forName("org.springframework.boot.SpringApplication");
            isSpringPresent = true;


            Annotation[] classAnnotations = testClass.getAnnotations();
            boolean hasEnableAutoConfigAnnotation = false;
            for (Annotation classAnnotation : classAnnotations) {
                if (classAnnotation.annotationType().getCanonicalName().startsWith("org.springframework.")) {
                    hasEnableAutoConfigAnnotation = true;
                    break;
                }
            }
            Class<?> testContextManagerClass = null;
            try {
                testContextManagerClass = Class.forName("org.springframework.test.context.TestContextManager");
            } catch (Exception e) {
            }
            // no spring context creation if no spring annotation is used on the test class
            if (!hasEnableAutoConfigAnnotation) {
                return;
            }


            this.springTestContextManager = testContextManagerClass.getConstructor(Class.class).newInstance(testClass);
            Method getTestContextMethod = testContextManagerClass.getMethod("getTestContext");
            Class<?> testContextClass = Class.forName("org.springframework.test.context.TestContext");

            Method getApplicationContextMethod = testContextClass.getMethod("getApplicationContext");


            Class<?> pspcClass = Class.forName(
                    "org.springframework.context.support.PropertySourcesPlaceholderConfigurer");

            Object propertySourcesPlaceholderConfigurer = pspcClass.getConstructor().newInstance();


            Class<?> propertiesClass = Class.forName("java.util.Properties");
            Method pspcClassSetPropertiesMethod = pspcClass.getMethod("setProperties", propertiesClass);

//            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            Class<?> yamlPropertiesFactoryBeanClass = Class.forName(
                    "org.springframework.beans.factory.config.YamlPropertiesFactoryBean");
            Object yaml = yamlPropertiesFactoryBeanClass.getConstructor().newInstance();
            Method yamlGetObjectMethod = yamlPropertiesFactoryBeanClass.getMethod("getObject");
            Class<?> classPathResourceClass = Class.forName("org.springframework.core.io.ClassPathResource");
            Object classPathResource = classPathResourceClass.getConstructor(String.class)
                    .newInstance("config/application.yml");
//            ClassPathResource classPathResource = new ClassPathResource("config/application.yml");
            Method setResourceMethod = yamlPropertiesFactoryBeanClass.getMethod("setResources",
                    Class.forName("[Lorg.springframework.core.io.Resource;"));
            Method resourceExistsMethod = classPathResourceClass.getMethod("exists");
            if ((boolean) resourceExistsMethod.invoke(classPathResource)) {
//                yaml.setResources(classPathResource);
                setResourceMethod.invoke(yaml, classPathResource);
//                propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
                Object yamlObject = yamlGetObjectMethod.invoke(yaml);
                pspcClassSetPropertiesMethod.invoke(propertySourcesPlaceholderConfigurer, yamlObject);
            }


            Object testContext = getTestContextMethod.invoke(this.springTestContextManager);
            Object applicationContext = getApplicationContextMethod.invoke(testContext);

            Object factory = setSpringApplicationContextAndLoadBeanFactory(applicationContext);

            Method pspcProcessBeanFactoryMethod = pspcClass.getMethod("postProcessBeanFactory",
                    Class.forName("org.springframework.beans.factory.config.ConfigurableListableBeanFactory"));

            pspcProcessBeanFactoryMethod.invoke(propertySourcesPlaceholderConfigurer, factory);

//            propertySourcesPlaceholderConfigurer.postProcessBeanFactory(
//                    (DefaultListableBeanFactory) this.springTestContextManager.getTestContext().getApplicationContext()
//                            .getAutowireCapableBeanFactory());
        } catch (Throwable e) {
            // failed to start spring application context
            throw new RuntimeException("Failed to create spring application context", e);
        }
    }

    private Object setSpringApplicationContextAndLoadBeanFactory(Object applicationContext) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (applicationContext == null) {
            return null;
        }
        this.applicationContext = applicationContext;

        Class<?> applicationContextClass = Class.forName("org.springframework.context.ApplicationContext");
        getBeanMethod = applicationContextClass.getMethod("getBean", Class.class);
        Method getAutowireCapableBeanFactoryMethod = applicationContextClass.getMethod("getAutowireCapableBeanFactory");

        springBeanFactory = Class.forName("org.springframework.beans.factory.support.DefaultListableBeanFactory")
                .cast(getAutowireCapableBeanFactoryMethod.invoke(applicationContext));
        return springBeanFactory;
    }


    public void enableSpringIntegration(Class<?> testClass) {
        if (this.springTestContextManager == null) {
            trySpringIntegration(testClass);
        }
    }
}
