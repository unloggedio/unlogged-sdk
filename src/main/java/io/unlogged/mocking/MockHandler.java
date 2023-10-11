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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MockHandler {
    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final ByteBuddy byteBuddy;
    private final Objenesis objenesis;
    private final Object originalImplementation;
    private final Object originalFieldParent;
    private final Map<Integer, AtomicInteger> mockMatchCountMap = new HashMap<>();
    private final ClassLoader targetClassLoader;
    private final Field field;

    public MockHandler(
            List<DeclaredMock> declaredMocks,
            ObjectMapper objectMapper,
            ByteBuddy byteBuddy,
            Objenesis objenesis, Object originalImplementation,
            Object originalFieldParent, ClassLoader targetClassLoader, Field field) {
        this.objectMapper = objectMapper;
        this.byteBuddy = byteBuddy;
        this.objenesis = objenesis;
        this.originalImplementation = originalImplementation;
        this.originalFieldParent = originalFieldParent;
        this.targetClassLoader = targetClassLoader;
        this.field = field;

        addDeclaredMocks(declaredMocks);
    }

    public Field getField() {
        return field;
    }

    @RuntimeType
    @BindingPriority(value = 1000)
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
                        mockMatched = isParameterMatched(parameterMatcher, argument);
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
                    if (thenParameter.getMethodExitType() == MethodExitType.NULL) {
                        return null;
                    }
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
                                            String classNameToBeConstructed = returnParameter.getClassName();
                                            typeReference = getTypeReference(typeFactory, classNameToBeConstructed);
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
                                    byteBuddy, objenesis, originalImplementation, originalFieldParent,
                                    targetClassLoader,
                                    field);
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

    private JavaType getTypeReference(TypeFactory typeFactory, String classNameToBeConstructed) {
        if (classNameToBeConstructed.endsWith("[]")) {
            JavaType subType = getTypeReference(typeFactory, classNameToBeConstructed.substring(0,
                    classNameToBeConstructed.length() - 2));
            return typeFactory.constructArrayType(subType);
        }
        return typeFactory.constructFromCanonical(classNameToBeConstructed);
    }

    private boolean isParameterMatched(ParameterMatcher parameterMatcher, Object argument) throws ClassNotFoundException {
        boolean mockMatched = true;
        switch (parameterMatcher.getType()) {
            case ANY_OF_TYPE:

                TypeFactory typeFactory = objectMapper.getTypeFactory();
                JavaType expectedClassType;
                switch (parameterMatcher.getValue()) {
                    case "int":
                        expectedClassType = typeFactory.constructType(Integer.class);
                        break;
                    case "short":
                        expectedClassType = typeFactory.constructType(Short.class);
                        break;
                    case "float":
                        expectedClassType = typeFactory.constructType(Float.class);
                        break;
                    case "long":
                        expectedClassType = typeFactory.constructType(Long.class);
                        break;
                    case "byte":
                        expectedClassType = typeFactory.constructType(Byte.class);
                        break;
                    case "double":
                        expectedClassType = typeFactory.constructType(Double.class);
                        break;
                    case "boolean":
                        expectedClassType = typeFactory.constructType(Boolean.class);
                        break;
                    case "char":
                        expectedClassType = typeFactory.constructType(Character.class);
                        break;
                    default:
                        expectedClassType = getTypeReference(typeFactory, parameterMatcher.getValue());

                        break;
                }
                if (argument == null) {
                    // null gonna match
                    break;
                }

                JavaType actualJavaType = typeFactory.constructType(argument.getClass());
                if (!expectedClassType.getRawClass().isAssignableFrom(actualJavaType.getRawClass())) {
                    mockMatched = false;
                }
                break;
            case EQUAL:
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
            case ANY:
                // always matches
                break;
            case NULL:
                mockMatched = argument == null;
                break;
            case TRUE:
                mockMatched = argument == Boolean.TRUE;
                break;
            case FALSE:
                mockMatched = argument == Boolean.FALSE;
                break;
            case NOT_NULL:
                mockMatched = argument != null;
                break;
            case ANY_STRING:
                mockMatched = argument instanceof String;
                break;
            case STARTS_WITH:
                mockMatched = argument instanceof String &&
                        ((String) argument).startsWith(parameterMatcher.getValue());
                break;
            case ENDS_WITH:
                mockMatched = argument instanceof String &&
                        ((String) argument).endsWith(parameterMatcher.getValue());
                break;
            case MATCHES_REGEX:
                mockMatched = argument instanceof String &&
                        Pattern.compile(parameterMatcher.getValue())
                                .matcher((String) argument).matches();
                break;
            case ANY_SHORT:
                mockMatched = argument instanceof Short;
                break;
            case ANY_CHAR:
                mockMatched = argument instanceof Character;
                break;
            case ANY_FLOAT:
                mockMatched = argument instanceof Float;
                break;
            case ANY_DOUBLE:
                mockMatched = argument instanceof Double;
                break;
            case ANY_BYTE:
                mockMatched = argument instanceof Byte;
                break;
            case ANY_BOOLEAN:
                mockMatched = argument instanceof Boolean;
                break;
            case ANY_MAP:
                mockMatched = Map.class.isAssignableFrom(argument.getClass());
                break;
            case ANY_SET:
                mockMatched = Set.class.isAssignableFrom(argument.getClass());
                break;
            case ANY_LIST:
                mockMatched = List.class.isAssignableFrom(argument.getClass());
                break;
            default:
                throw new RuntimeException("Invalid " + parameterMatcher);
        }
        return mockMatched;
    }

    public void addDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.addAll(declaredMocksForField);
    }

    public void setDeclaredMocks(List<DeclaredMock> declaredMocksForField) {
        declaredMocks.clear();
        addDeclaredMocks(declaredMocksForField);
    }

    public Object getOriginalImplementation() {
        return originalImplementation;
    }

    public Object getOriginalFieldParent() {
        return originalFieldParent;
    }

    public void removeDeclaredMock(List<DeclaredMock> mocksToRemove) {
        this.declaredMocks.removeAll(mocksToRemove);
    }
}
