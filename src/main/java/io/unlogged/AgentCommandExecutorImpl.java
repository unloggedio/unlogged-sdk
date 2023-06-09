package io.unlogged;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.unlogged.command.*;
import io.unlogged.logging.IEventLogger;
import io.unlogged.util.ClassTypeUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AgentCommandExecutorImpl implements AgentCommandExecutor {

    final private ObjectMapper objectMapper;
    final private IEventLogger logger;

    public AgentCommandExecutorImpl(ObjectMapper objectMapper, IEventLogger logger) {
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    private static void closeHibernateSessionIfPossible(Object sessionInstance)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (sessionInstance != null) {

            Method getTransactionMethod = sessionInstance.getClass().getMethod("getTransaction");
            Object transactionInstance = getTransactionMethod.invoke(sessionInstance);
//            System.err.println("Transaction to commit: " + transactionInstance);
            Method rollbackMethod = transactionInstance.getClass().getMethod("rollback");
            rollbackMethod.invoke(transactionInstance);


            Method sessionCloseMethod = sessionInstance.getClass().getMethod("close");
            sessionCloseMethod.invoke(sessionInstance);
        }
    }

    @Override
    public AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) throws Exception {
//        System.err.println("AgentCommandRequest: " + agentCommandRequest);

        AgentCommandRequestType requestType = agentCommandRequest.getRequestType();
        if (requestType == null) {
            requestType = AgentCommandRequestType.REPEAT_INVOKE;
        }
        try {
            if (requestType.equals(AgentCommandRequestType.REPEAT_INVOKE)) {
                logger.setRecording(true);
            }
            Object sessionInstance = tryOpenHibernateSessionIfHibernateExists();
            try {


                Class<?> objectClass;
                ClassLoader targetClassLoader;

                Object objectInstanceByClass = null;

                objectInstanceByClass = logger.getObjectByClassName(agentCommandRequest.getClassName());
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
                    objectInstanceByClass = tryObjectConstruct(agentCommandRequest.getClassName(), targetClassLoader1);
//                    if (objectInstanceByClass == null) {
//                        throw new NoSuchMethodException(
//                                "Instance of class [" + agentCommandRequest.getClassName() + "] " +
//                                        "not found and could not be created");
//                    }
                }

                objectClass = objectInstanceByClass != null ? objectInstanceByClass.getClass() :
                        Class.forName(agentCommandRequest.getClassName(), false, targetClassLoader1);

                targetClassLoader = objectInstanceByClass != null ?
                        objectInstanceByClass.getClass().getClassLoader() : targetClassLoader1;

                Method methodToExecute = null;

                List<String> methodSignatureParts = ClassTypeUtil.splitMethodDesc(
                        agentCommandRequest.getMethodSignature());

                // do not remove this transformation
                String methodReturnType = methodSignatureParts.remove(methodSignatureParts.size() - 1);

                List<String> methodParameters = agentCommandRequest.getMethodParameters();

                Class<?>[] methodParameterTypes = new Class[methodSignatureParts.size()];

                for (int i = 0; i < methodSignatureParts.size(); i++) {
                    String methodSignaturePart = methodSignatureParts.get(i);
//                System.err.println("Method parameter [" + i + "] type: " + methodSignaturePart);
                    methodParameterTypes[i] =
                            ClassTypeUtil.getClassNameFromDescriptor(methodSignaturePart, targetClassLoader);
                }


                List<Method> methodList = new ArrayList<>();
                while (objectClass != null && !objectClass.equals(Object.class)) {

                    try {
                        methodToExecute = objectClass
                                .getMethod(agentCommandRequest.getMethodName(), methodParameterTypes);
                    } catch (NoSuchMethodException noSuchMethodException) {

                    }

                    if (methodToExecute == null) {
                        Method[] methods = objectClass.getDeclaredMethods();
                        for (Method method : methods) {
                            methodList.add(method);
                            if (method.getName().equals(agentCommandRequest.getMethodName())
                                    && method.getParameterCount() == methodParameters.size()) {
                                methodToExecute = method;
                                break;
                            }
                        }
                    }
                    if (methodToExecute != null) {
                        break;
                    }
                    objectClass = objectClass.getSuperclass();
                }

                if (methodToExecute == null) {
                    List<String> methodNamesList = methodList.stream()
                            .map(Method::getName)
                            .collect(Collectors.toList());
                    System.err.println("Method not found: " + agentCommandRequest.getMethodName()
                            + ", methods were: " + methodNamesList);
                    throw new NoSuchMethodException("method not found [" + agentCommandRequest.getMethodName()
                            + "] in class [" + agentCommandRequest.getClassName() + "]. Available methods are: "
                            + methodNamesList);
                }


                methodToExecute.setAccessible(true);


                Class<?>[] parameterTypesClass = methodToExecute.getParameterTypes();
                Object[] parameters = new Object[methodParameters.size()];
                TypeFactory typeFactory = objectMapper.getTypeFactory().withClassLoader(targetClassLoader);

                List<String> parameterTypes = agentCommandRequest.getParameterTypes();
                for (int i = 0; i < methodParameters.size(); i++) {
                    String methodParameter = methodParameters.get(i);
                    Class<?> parameterType = parameterTypesClass[i];
//                    System.err.println("Make value of type [" + parameterType + "] from value: " + methodParameter);
                    Object parameterObject;
                    if (parameterType.getCanonicalName().equals("org.springframework.util.MultiValueMap")) {
                        parameterObject = objectMapper.readValue(methodParameter,
                                Class.forName("org.springframework.util.LinkedMultiValueMap"));
                    } else {

                        JavaType typeReference;
                        try {
                            typeReference = typeFactory.constructFromCanonical(parameterTypes.get(i));
                        } catch (Exception e) {
                            // failed to construct from the canonical name,
                            // happens when this is a generic type
                            // so we try to construct using type from the method param class
                            typeReference = typeFactory.constructType(parameterType);
                        }
                        parameterObject = objectMapper.readValue(methodParameter, typeReference);
                    }

                    parameters[i] = parameterObject;
                }


                AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
                agentCommandResponse.setTargetClassName(agentCommandRequest.getClassName());
                agentCommandResponse.setTargetMethodName(agentCommandRequest.getMethodName());
                agentCommandResponse.setTargetMethodSignature(agentCommandRequest.getMethodSignature());
                agentCommandResponse.setTimestamp(new Date().getTime());

                try {
                    Object methodReturnValue = methodToExecute.invoke(objectInstanceByClass, parameters);

                    if (methodReturnValue instanceof Double) {
                        agentCommandResponse.setMethodReturnValue(Double.doubleToLongBits((Double) methodReturnValue));
                    } else if (methodReturnValue instanceof Float) {
                        agentCommandResponse.setMethodReturnValue(Float.floatToIntBits((Float) methodReturnValue));
                    } else if (methodReturnValue instanceof Flux) {
                        Flux<?> returnedFlux = (Flux<?>) methodReturnValue;
                        agentCommandResponse.setMethodReturnValue(
                                objectMapper.writeValueAsString(returnedFlux.collectList().block()));
                    } else if (methodReturnValue instanceof Mono) {
                        Mono<?> returnedFlux = (Mono<?>) methodReturnValue;
                        agentCommandResponse.setMethodReturnValue(
                                objectMapper.writeValueAsString(returnedFlux.block()));
                    } else {
                        agentCommandResponse.setMethodReturnValue(objectMapper.writeValueAsString(methodReturnValue));
                    }

                    agentCommandResponse.setResponseClassName(methodToExecute.getReturnType().getCanonicalName());
                    agentCommandResponse.setResponseType(ResponseType.NORMAL);
                } catch (Throwable exception) {
                    if (exception instanceof InvocationTargetException) {
                        exception.getCause().printStackTrace();
                    } else {
                        exception.printStackTrace();
                    }
                    Throwable exceptionCause = exception.getCause() != null ? exception.getCause() : exception;
                    agentCommandResponse.setMessage(exceptionCause.getMessage());
                    try {
                        agentCommandResponse.setMethodReturnValue(objectMapper.writeValueAsString(exceptionCause));
                    } catch (Throwable e) {
                        agentCommandResponse.setMethodReturnValue("Exception: " + exceptionCause.getMessage());
                        agentCommandResponse.setMessage("Exception: " + exceptionCause.getMessage());
                        // failed to serialize thrown exception
                    }
                    agentCommandResponse.setResponseClassName(exceptionCause.getClass().getCanonicalName());
                    agentCommandResponse.setResponseType(ResponseType.EXCEPTION);
                }
                return agentCommandResponse;
            } finally {
                closeHibernateSessionIfPossible(sessionInstance);
            }
        } finally {
            if (requestType.equals(AgentCommandRequestType.REPEAT_INVOKE)) {
                logger.setRecording(false);
            }
        }


    }


    private Object tryObjectConstruct(String className, ClassLoader targetClassLoader)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (targetClassLoader == null) {
            System.err.println("Failed to construct instance of class [" + className + "]. classLoader is not defined");
        }
        Class<?> loadedClass = targetClassLoader.loadClass(className);
        Constructor<?> noArgsConstructor = null;
        try {
            noArgsConstructor = loadedClass.getConstructor();
            try {
                return noArgsConstructor.newInstance();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
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
            throw new RuntimeException(e);
        }
    }


    private Object tryOpenHibernateSessionIfHibernateExists() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Object hibernateSessionFactory = logger.getObjectByClassName("org.hibernate.internal.SessionFactoryImpl");
        Object sessionInstance = null;
        if (hibernateSessionFactory != null) {
//            System.err.println("Hibernate session factory: " + hibernateSessionFactory);
            Method openSessionMethod = hibernateSessionFactory.getClass().getMethod("openSession");
            sessionInstance = openSessionMethod.invoke(hibernateSessionFactory);
//            System.err.println("Hibernate session opened: " + sessionInstance);
            Class<?> managedSessionContextClass = Class.forName("org.hibernate.context.internal.ManagedSessionContext");
            Method bindMethod = managedSessionContextClass.getMethod("bind", Class.forName("org.hibernate.Session"));
            bindMethod.invoke(null, sessionInstance);


            Method beginTransactionMethod = sessionInstance.getClass().getMethod("beginTransaction");
            beginTransactionMethod.invoke(sessionInstance);
        }
        return sessionInstance;
    }

}
