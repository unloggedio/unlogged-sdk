package io.unlogged.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import org.objenesis.Objenesis;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MockHandler {
    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private final Map<String, DeclaredMock> declaredMocksMap = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final ByteBuddy byteBuddy;
    private final Objenesis objenesis;
    private final Object originalImplementation;
    private final Map<Integer, AtomicInteger> mockMatchCountMap = new HashMap<>();

    public MockHandler(
            List<DeclaredMock> declaredMocks,
            ObjectMapper objectMapper,
            ByteBuddy byteBuddy,
            Objenesis objenesis, Object originalImplementation) {
        this.objectMapper = objectMapper;
        this.byteBuddy = byteBuddy;
        this.objenesis = objenesis;
        this.originalImplementation = originalImplementation;

        addDeclaredMocks(declaredMocks);
    }


    @RuntimeType
    public Object intercept(@AllArguments Object[] methodArguments,
                            @This Object thisInstance,
                            @Origin Method invokedMethod,
                            @Super Object superInstance
    ) throws Throwable {
        String methodName = invokedMethod.getName();

        for (DeclaredMock declaredMock : declaredMocks) {
            if (declaredMock.getMethodName().equals(methodName)) {
                boolean mockMatched = true;
//                System.out.println("Expected vs actual parameter size: " + declaredMock.getWhenParameter().size() +
//                        ", " + methodArguments.length);
                if (declaredMock.getWhenParameter().size() == methodArguments.length) {
                    List<ParameterMatcher> whenParameter = declaredMock.getWhenParameter();
                    for (int i = 0; i < whenParameter.size(); i++) {
                        ParameterMatcher parameterMatcher = whenParameter.get(i);
                        Object argument = methodArguments[i];
//                        System.out.println("Parameter matcher: " + parameterMatcher);
                        switch (parameterMatcher.getType()) {
                            case ANY:
                                if (!argument.getClass().getCanonicalName().equals(parameterMatcher.getValue())) {
                                    mockMatched = false;
                                }
                                break;
                            case IS:
                                try {
                                    JsonNode argumentAsJsonNode = objectMapper.readTree(
                                            objectMapper.writeValueAsString(argument));
                                    JsonNode expectedJsonNode = objectMapper.readTree(parameterMatcher.getValue());
                                    if (expectedJsonNode.equals(argumentAsJsonNode)) {
                                        mockMatched = false;
                                    }
                                } catch (JsonProcessingException e) {
                                    // lets just compare as string
                                    if (!Objects.equals(argument, parameterMatcher.getValue())) {
                                        mockMatched = false;
                                    }
                                }
                                break;
                            default:
//                                System.out.println("Invalid parameter matcher: " + parameterMatcher);
                                throw new RuntimeException("Invalid " + parameterMatcher);
                        }
                        if (!mockMatched) {
//                            System.out.println("Parameter mismatch: " + parameterMatcher);
                            break;
                        }
                    }
                } else {
                    mockMatched = false;
                }
//                System.out.println("Intercepted call to mock: " + thisInstance.getClass().getName() + "." + methodName + "()");
                if (mockMatched) {
                    Object returnValueInstance = null;
                    int mockHash = declaredMock.hashCode();
                    AtomicInteger matchCount = mockMatchCountMap.getOrDefault(mockHash, new AtomicInteger(0));
                    List<ThenParameter> thenParameterList = declaredMock.getThenParameter();
                    int selectedThenParameter = Math.min(matchCount.getAndIncrement(), thenParameterList.size() - 1);
                    ThenParameter thenParameter = thenParameterList.get(selectedThenParameter);
                    mockMatchCountMap.put(mockHash, matchCount);
                    ReturnValue returnParameter = thenParameter.getReturnParameter();
                    switch (returnParameter.getReturnValueType()) {
                        case REAL:
                            try {
                                if (thenParameter.getMethodExitType() == MethodExitType.NORMAL) {
                                    if (returnParameter.getValue() != null && returnParameter.getValue().length() > 0) {

                                        TypeFactory typeFactory = objectMapper.getTypeFactory()
                                                .withClassLoader(thisInstance.getClass().getClassLoader());


                                        JavaType typeReference;
                                        try {
                                            typeReference = typeFactory.constructFromCanonical(
                                                    returnParameter.getClassName());
                                        } catch (Exception e) {
                                            // failed to construct from the canonical name,
                                            // happens when this is a generic type
                                            // so we try to construct using type from the method param class
                                            typeReference = typeFactory.constructType(invokedMethod.getReturnType());
                                        }

                                        returnValueInstance = objectMapper.readValue(returnParameter.getValue(),
                                                typeReference);
                                    }
                                } else {
                                    // this is an instance of exception class
                                    Class<?> exceptionClassType = thisInstance.getClass().getClassLoader()
                                            .loadClass(returnParameter.getClassName());
                                    try {
                                        Constructor<?> constructorWithMessage = exceptionClassType.getConstructor(
                                                String.class);
                                        returnValueInstance =
                                                constructorWithMessage.newInstance(returnParameter.getValue());
                                    } catch (Exception e) {
                                        returnValueInstance = exceptionClassType.getConstructor().newInstance();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("Failed to create instance of class [" +
                                        returnParameter.getClassName() + "] from value [" + returnParameter.getValue() + "] => " + e.getMessage());
                            }
                            break;
                        case MOCK:
                            MockHandler mockHandler = new MockHandler(returnParameter.getDeclaredMocks(), objectMapper,
                                    byteBuddy, objenesis, originalImplementation);
                            Class<?> fieldType = invokedMethod.getReturnType();
                            DynamicType.Loaded<?> loadedMockedField = byteBuddy
                                    .subclass(fieldType)
                                    .method(isDeclaredBy(fieldType)).intercept(MethodDelegation.to(mockHandler))
                                    .make()
                                    .load(thisInstance.getClass().getClassLoader());

                            returnValueInstance = objenesis.newInstance(loadedMockedField.getLoaded());
                            break;
                        default:
                            throw new RuntimeException("Unknown return parameter type => " + returnParameter);
                    }
                    switch (thenParameter.getMethodExitType()) {
                        case NORMAL:
                            return returnValueInstance;
                        case EXCEPTION:
                            if (returnValueInstance == null) {
                                returnValueInstance =
                                        new Exception(
                                                "Object to be thrown from mock is null: " + declaredMock);
                            }
                            throw (Throwable) returnValueInstance;
                    }
                }
            }
        }

//        System.out.println("Invoke method [" + invokedMethod.getName() + " on " + invokedMethod.getDeclaringClass()
//                .getCanonicalName() + "] with args [" + Arrays.asList(methodArguments)
//                + "] on " + superInstance.getClass().getCanonicalName() + " from " + thisInstance.getClass().getCanonicalName());

        Method realMethod = originalImplementation.getClass()
                .getMethod(invokedMethod.getName(), invokedMethod.getParameterTypes());
        return realMethod.invoke(originalImplementation, methodArguments);
    }

    public void addDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.addAll(declaredMocksForField);
    }

    public void setDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.clear();
        declaredMocksMap.clear();
        addDeclaredMocks(declaredMocksForField);
    }
}
