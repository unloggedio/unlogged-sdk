package io.unlogged.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.unlogged.ParameterFactory;
import io.unlogged.mocking.construction.JsonDeserializer;
import net.bytebuddy.implementation.bind.annotation.*;
import org.objenesis.Objenesis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class MockHandler {
    private final ObjectMapper objectMapper;


    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private final Objenesis objenesis;
    private final Object originalImplementation;
    private final Object originalFieldParent;
    private final Map<Integer, AtomicInteger> mockMatchCountMap = new HashMap<>();
    private final ClassLoader targetClassLoader;
    private final Field field;

    private final JsonDeserializer jsonDeserializer;
    private final ParameterFactory parameterFactory;

    public MockHandler(
            List<DeclaredMock> declaredMocks,
            ObjectMapper objectMapper,
            ParameterFactory parameterFactory,
            Objenesis objenesis,
            Object originalImplementation,
            Object originalFieldParent,
            ClassLoader targetClassLoader,
            Field field) {
        this.objectMapper = objectMapper;
        this.jsonDeserializer = new JsonDeserializer(objectMapper);
        this.parameterFactory = parameterFactory;
        this.objenesis = objenesis;
        this.originalImplementation = originalImplementation;
        this.originalFieldParent = originalFieldParent;
        this.targetClassLoader = targetClassLoader;
        this.field = field;

        addDeclaredMocks(declaredMocks);
    }

    public static JavaType getTypeReference(TypeFactory typeFactory, String classNameToBeConstructed) {
        if (classNameToBeConstructed == null) {
            return null;
        }
        if (classNameToBeConstructed.endsWith("[]")) {
            JavaType subType = getTypeReference(typeFactory, classNameToBeConstructed.substring(0,
                    classNameToBeConstructed.length() - 2));
            return typeFactory.constructArrayType(subType);
        }
        switch (classNameToBeConstructed) {
            case "J":
            case "long":
                return typeFactory.constructType(long.class);
            case "Z":
            case "boolean":
                return typeFactory.constructType(boolean.class);
            case "I":
            case "integer":
                return typeFactory.constructType(int.class);
            case "B":
            case "byte":
                return typeFactory.constructType(byte.class);
            case "C":
            case "char":
                return typeFactory.constructType(char.class);
            case "F":
            case "float":
                return typeFactory.constructType(float.class);
            case "S":
            case "short":
                return typeFactory.constructType(short.class);
            case "D":
            case "double":
                return typeFactory.constructType(double.class);
            case "V":
            case "void":
                return typeFactory.constructType(void.class);
        }
        return typeFactory.constructFromCanonical(classNameToBeConstructed);
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
                    ClassLoader classLoader = thisInstance.getClass().getClassLoader();
                    switch (returnParameter.getReturnValueType()) {
                        case REAL:
                            try {

                                returnValueInstance = parameterFactory.createObjectInstanceFromStringAndTypeInformation(
                                        returnParameter.getClassName(), returnParameter.getValue(),
                                        invokedMethod.getReturnType(),
                                        objectMapper.getTypeFactory().withClassLoader(targetClassLoader)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("Failed to create instance of class [" +
                                        returnParameter.getClassName() + "] from value [" + returnParameter.getValue() + "] => " + e.getMessage());
                            }
                            break;
                        case MOCK:

                            Class<?> fieldType = invokedMethod.getReturnType();

                            MockInstance mockedField = parameterFactory.createMockedInstance(
                                    targetClassLoader, originalFieldParent, field, returnParameter.getDeclaredMocks(),
                                    null, fieldType
                            );

                            returnValueInstance = mockedField.getMockedFieldInstance();
                            break;
                        default:
                            throw new RuntimeException("Unknown return parameter type => " + returnParameter);
                    }
                    switch (thenParameter.getMethodExitType()) {
                        case NORMAL:
                            return returnValueInstance;
                        case EXCEPTION:
                            if (returnValueInstance == null) {
                                returnValueInstance = new Exception("Object to be thrown from mock is null: " + declaredMock);
                            }
                            throw (Throwable) returnValueInstance;
                    }
                }
            }
        }

//        System.out.println("Invoke method [" + invokedMethod.getName() + " on " + invokedMethod.getDeclaringClass()
//                .getCanonicalName() + "] with args [" + Arrays.asList(methodArguments)
//                + "] on " + superInstance.getClass().getCanonicalName() + " from " + thisInstance.getClass().getCanonicalName());

//        if (originalImplementation == null) {
//            if (field != null) {
//                System.err.println(
//                        "originalImplementation is null " + field.getType().getCanonicalName() + " " + field.getName());
//            } else {
//                System.err.println("originalImplementation is null");
//            }
//        }
//        Method realMethod = originalImplementation.getClass()
//                .getMethod(invokedMethod.getName(), invokedMethod.getParameterTypes());

        if (originalImplementation == null) {
            return null;
        }

        return invokedMethod.invoke(originalImplementation, methodArguments);
    }


    private boolean isParameterMatched(ParameterMatcher parameterMatcher, Object argument) {
        boolean mockMatched = true;
        switch (parameterMatcher.getType()) {
            case ANY_OF_TYPE:

                TypeFactory typeFactory = objectMapper.getTypeFactory();
                JavaType expectedClassType;
                switch (parameterMatcher.getValue()) {
                    case "int":
                        expectedClassType = typeFactory.constructType(int.class);
                        break;
                    case "short":
                        expectedClassType = typeFactory.constructType(short.class);
                        break;
                    case "float":
                        expectedClassType = typeFactory.constructType(float.class);
                        break;
                    case "long":
                        expectedClassType = typeFactory.constructType(long.class);
                        break;
                    case "byte":
                        expectedClassType = typeFactory.constructType(byte.class);
                        break;
                    case "double":
                        expectedClassType = typeFactory.constructType(double.class);
                        break;
                    case "boolean":
                        expectedClassType = typeFactory.constructType(boolean.class);
                        break;
                    case "char":
                        expectedClassType = typeFactory.constructType(char.class);
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

                if (expectedClassType.isPrimitive() || actualJavaType.isPrimitive()) {
                    Class<?> primitiveExpectedType = getPrimitiveType(expectedClassType);
                    Class<?> primitiveActualType = getPrimitiveType(expectedClassType);
                    mockMatched = Objects.equals(primitiveExpectedType, primitiveActualType);
                } else if (!expectedClassType.getRawClass().isAssignableFrom(actualJavaType.getRawClass())) {
                    mockMatched = false;
                }
                break;
            case EQUAL:
                try {
                    JsonNode argumentAsJsonNode = objectMapper.readTree(
                            objectMapper.writeValueAsString(argument));
                    JsonNode expectedJsonNode = objectMapper.readTree(parameterMatcher.getValue());
                    if (!expectedJsonNode.equals(argumentAsJsonNode)) {
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

    private Class<?> getPrimitiveType(JavaType expectedClassType) {
        Class<?> rawClass = expectedClassType.getRawClass();
        if (expectedClassType.isPrimitive()) {
            return rawClass;
        }
        if (rawClass.equals(Byte.class)) {
            return byte.class;
        }
        if (rawClass.equals(Integer.class)) {
            return int.class;
        }
        if (rawClass.equals(Boolean.class)) {
            return boolean.class;
        }
        if (rawClass.equals(Character.class)) {
            return char.class;
        }
        if (rawClass.equals(Float.class)) {
            return float.class;
        }
        if (rawClass.equals(Long.class)) {
            return long.class;
        }
        if (rawClass.equals(Short.class)) {
            return short.class;
        }
        if (rawClass.equals(Double.class)) {
            return double.class;
        }
        if (rawClass.equals(Void.class)) {
            return void.class;
        }
        return null;
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
