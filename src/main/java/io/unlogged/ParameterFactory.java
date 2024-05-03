package io.unlogged;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.unlogged.mocking.*;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.objenesis.Objenesis;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

public class ParameterFactory {


    public static final String MULTI_VALUE_MAP_CLASS = "org.springframework.util.MultiValueMap";
    public static final String LINKED_MULTI_VALUE_MAP = "org.springframework.util.LinkedMultiValueMap";
    private final Objenesis objenesis;
    private final ObjectMapper objectMapper;
    private final ByteBuddy byteBuddyInstance;
    private ObjectMapper basicObjectMapper = new ObjectMapper();


    public ParameterFactory(Objenesis objenesis, ObjectMapper objectMapper, ByteBuddy byteBuddyInstance) {
        this.byteBuddyInstance = byteBuddyInstance;
        this.objectMapper = objectMapper;
        this.objenesis = objenesis;
    }

    public Class<?>[] getAllInterfaces(Object o) {
        try {
            Set<Class<?>> results = new HashSet<>();
            getAllInterfaces(o, results::add);
            return results.toArray(new Class<?>[0]);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    public void getAllInterfaces(Object o, Function<Class<?>, Boolean> accumulator) throws IllegalArgumentException {
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

    public Object createParameterUsingObjenesis(JavaType typeReference, String methodParameter)
            throws JsonProcessingException, IllegalAccessException {
        Class<?> rawClass = typeReference.getRawClass();
        Object parameterObject = objenesis.newInstance(rawClass);
        Class<?> currentClass = rawClass;
        JsonNode providedValues = objectMapper.readTree(methodParameter);
        while (!currentClass.equals(Object.class)) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                JsonNode fieldValueInNodeByName = providedValues.get(declaredField.getName());
                Object valueToSet = getValueToSet(fieldValueInNodeByName, declaredField.getType());
                if (valueToSet == null) {
                    continue;
                }
                declaredField.setAccessible(true);
                declaredField.set(parameterObject, valueToSet);
            }
            currentClass = currentClass.getSuperclass();
        }
        return parameterObject;
    }

    private Object getValueToSet(JsonNode fieldValueInNodeByName, Class<?> type) {
        if (fieldValueInNodeByName == null) {
            return null;
        }
        Object valueToSet = null;
        if (int.class.equals(type) || Integer.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.intValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Integer.parseInt(fieldValueInNodeByName.textValue());
            }

        } else if (long.class.equals(type) || Long.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.longValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Long.parseLong(fieldValueInNodeByName.textValue());
            }

        } else if (double.class.equals(type) || Double.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.doubleValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Double.parseDouble(fieldValueInNodeByName.textValue());
            }

        } else if (float.class.equals(type) || Float.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.floatValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Float.parseFloat(fieldValueInNodeByName.textValue());
            }

        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.booleanValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Boolean.parseBoolean(fieldValueInNodeByName.textValue());
            }

        } else if (short.class.equals(type) || Short.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.shortValue();
            if (fieldValueInNodeByName instanceof TextNode) {
                valueToSet = Short.parseShort(fieldValueInNodeByName.textValue());
            }

        } else if (String.class.equals(type)) {
            valueToSet = fieldValueInNodeByName.textValue();
        } else if (StringBuilder.class.equals(type)) {
            valueToSet = new StringBuilder(fieldValueInNodeByName.textValue());
        } else {
            String valAsJsonString = fieldValueInNodeByName.toString();
            if (fieldValueInNodeByName instanceof TextNode) {
                valAsJsonString = fieldValueInNodeByName.textValue();
            }
            valueToSet = createObjectInstanceFromStringAndTypeInformation(
                    null, valAsJsonString, type, objectMapper.getTypeFactory());
        }
        return valueToSet;
    }

    public Object createObjectInstanceFromStringAndTypeInformation(
            String targetClassName, String objectJsonRepresentation, Class<?> parameterType, TypeFactory typeFactory) {
        Object parameterObject = null;
        if (parameterType.getCanonicalName().equals(MULTI_VALUE_MAP_CLASS)) {
            try {
                parameterObject = objectMapper.readValue(objectJsonRepresentation,
                        Class.forName(LINKED_MULTI_VALUE_MAP));
                return parameterObject;
            } catch (ClassNotFoundException | JsonProcessingException e) {
                // this should never happen
            }
        }
        JavaType typeReference = null;

        try {
            if (targetClassName != null) {
                typeReference = MockHandler.getTypeReference(typeFactory, targetClassName);
            }
        } catch (Throwable e1) {
            // failed to construct from the canonical name,
            // happens when this is a generic type
            // so we try to construct using type from the method param class

        }

        if (typeReference == null) {
            typeReference = typeFactory.constructType(parameterType);
        }

        try {
            parameterObject = objectFromTypeReference(objectJsonRepresentation, parameterType, typeReference);
        } catch (ClassNotFoundException | JsonProcessingException e) {
            //
        }

        return parameterObject;
    }

    private Object objectFromTypeReference(String methodParameter, Class<?> parameterType, final JavaType typeReference) throws JsonProcessingException, ClassNotFoundException {
        Object parameterObject;
        String rawClassCanonicalName = typeReference != null ? typeReference.getRawClass().getCanonicalName() : "java" +
                ".util.String";
        JavaType firstComponent = typeReference != null && typeReference.containedTypeCount() > 0 ?
                typeReference.containedType(0) : null;
        switch (rawClassCanonicalName) {
            case "reactor.core.publisher.Mono":

                parameterObject = objectFromTypeReference(methodParameter, parameterType, firstComponent);
                parameterObject = parameterObject == null ? Mono.empty() : Mono.just(parameterObject);

                break;
            case "java.util.concurrent.CompletableFuture":
                final Object finalObj = objectFromTypeReference(methodParameter, parameterType, firstComponent);
                parameterObject = CompletableFuture.supplyAsync(() -> finalObj);
                break;
            case "java.util.Optional":
                parameterObject = objectFromTypeReference(methodParameter, parameterType, firstComponent);
                parameterObject = parameterObject == null ? Optional.empty() : Optional.of(parameterObject);
                break;
            case "reactor.core.publisher.Flux":
                CollectionType actuallyComponent = objectMapper.getTypeFactory()
                        .constructCollectionType(ArrayList.class, firstComponent);
                List<?> parameterObjectList = (List<?>) objectFromTypeReference(methodParameter, parameterType,
                        actuallyComponent);
                parameterObject = parameterObjectList == null ? Flux.empty() : Flux.fromIterable(parameterObjectList);
                break;
            default:
                try {
                    if (methodParameter.equals("null")) {
                        return null;
                    }
                    parameterObject = objectMapper.readValue(methodParameter, typeReference);
                } catch (Throwable e2) {
                    try {
                        parameterObject = basicObjectMapper.readValue(methodParameter, typeReference);
                        return parameterObject;
                    }catch (Throwable ignored) {
                        //
                    }
                    if (methodParameter.startsWith("\"") && methodParameter.endsWith("\"")) {
                        try {
                            parameterObject = objectMapper.readValue(methodParameter.substring(
                                    1, methodParameter.length() - 1
                            ), typeReference);
                            return parameterObject;
                        } catch (Exception ingored) {

                        }
                    }
                    // a complicated type (no default args constructor), or interface which jackson cannot create ?
                    try {
                        // can we try using objenesis ?
                        parameterObject = createParameterUsingObjenesis(typeReference, methodParameter);
                        // we might want to now construct the whole object tree deep down
                    } catch (Throwable e3) {
                        // constructing using objenesis also failed
                        // lets try extending or implementing the class ?
                        parameterObject = createParameterUsingMocking(methodParameter, parameterType);
                    }
                }

                break;
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
                String potentialFieldName = methodName;
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

                potentialFieldName = potentialFieldName.substring(0, 1).toLowerCase() + potentialFieldName.substring(1);

                if (providedValues.has(potentialFieldName)) {
                    JsonNode providedValue = providedValues.get(potentialFieldName);
//                                Object valueToReturn = getValueToSet(typeFactory, providedValue, valueType);

                    if (methodName.startsWith("get") || methodName.startsWith("is")) {
                        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();

                        ReturnValue returnParameter;
                        if (checkCanClassBeExtended(valueType)) {
                            returnParameter = new ReturnValue(
                                    providedValue instanceof TextNode ?
                                            providedValue.textValue() : providedValue.toString(),
                                    valueType.getCanonicalName(),
                                    ReturnValueType.MOCK);
                        } else {
                            returnParameter = new ReturnValue(providedValue.toString(), valueType.getCanonicalName(),
                                    ReturnValueType.REAL);
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

    public MockInstance createMockedInstance(
            ClassLoader targetClassLoader,
            Object fieldParentInstance, Field field,
            List<DeclaredMock> parameterCallMocks,
            Object existingFieldInstance, Class<?> parameterType) {
        ClassLoadingStrategy<ClassLoader> strategy = ClassLoadingStrategy.Default.INJECTION;
        MockInstance existingMockInstance;
        if (fieldParentInstance == null) {
            if (field != null) {
                System.out.println(
                        "objectInstanceByClass is null: " + field.getType().getCanonicalName() + " " + field.getName());
            } else {
            }
        }
        MockHandler mockHandler = new MockHandler(parameterCallMocks, objectMapper,
                this, objenesis, existingFieldInstance, fieldParentInstance, targetClassLoader, field);

        DynamicType.Loaded<?> loadedMockedField;
        if (parameterType.isInterface()) {
            Class<?>[] implementedInterfaces = getAllInterfaces(parameterType);
            Set<String> implementedClasses = new HashSet<>();
            implementedClasses.add(parameterType.getCanonicalName());

            List<Class<?>> pendingImplementations = new ArrayList<>();
            pendingImplementations.add(parameterType);
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
                    .subclass(parameterType)
                    .method(isDeclaredBy(parameterType)).intercept(MethodDelegation.to(mockHandler))
                    .make()
                    .load(targetClassLoader, strategy);
        }


        Object mockedFieldInstance = objenesis.newInstance(loadedMockedField.getLoaded());

        existingMockInstance = new MockInstance(mockedFieldInstance, mockHandler);
        return existingMockInstance;
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


}
