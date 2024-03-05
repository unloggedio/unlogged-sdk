package io.unlogged;

import java.util.*;

public class MethodSignatureParser {

    public static List<String> parseMethodSignature(String signature) {
        ParseInfo parseInfo;
        parseInfo = new ParseInfo("", 1); // Reset index for each call
        List<String> parameterTypes = new ArrayList<>();
        while (signature.charAt(parseInfo.getNewIndex()) != ')') {
            parseInfo = parseType(signature, parseInfo.getNewIndex());
            parameterTypes.add(parseInfo.getValue());
        }
        ParseInfo returnInfo = parseType(signature, parseInfo.getNewIndex() + 1);
        parameterTypes.add(returnInfo.getValue());
        return parameterTypes;
    }

    private static ParseInfo parseType(String signature, int index) {
        switch (signature.charAt(index)) {
            case 'L':
                return parseComplexType(signature, index);
            case '[':
                return parseArrayType(signature, index);
            // Add cases for primitives here if needed
            default:
                // Advance for primitives or unsupported types
                String value = String.valueOf(signature.charAt(index));
                return new ParseInfo(value, index + 1);
        }
    }

    private static ParseInfo parseComplexType(String signature, int index) {
        index++; // Skip 'L'
        StringBuilder type = new StringBuilder();
        while (index < signature.length() && signature.charAt(index) != ';') {
            if (signature.charAt(index) == '<') {
                ParseInfo obj = parseGenericType(signature, index);
                type.append(obj.getValue());
                index = obj.getNewIndex();
            } else {
                type.append(signature.charAt(index));
                index++;
            }
        }
        index++; // Skip ';'
        return new ParseInfo(type.toString().replace('/', '.'), index);
    }

    private static ParseInfo parseGenericType(String signature, int index) {
        StringBuilder generic = new StringBuilder();
        index++; // Skip '<'
        generic.append('<');
        while (signature.charAt(index) != '>') {
            ParseInfo obj = parseType(signature, index);
            generic.append(obj.getValue());
            index = obj.getNewIndex();
            if (signature.charAt(index) == ',') {
                index++;
            }
            if (signature.charAt(index) != '>') {
                generic.append(", ");
            }
        }
        index++; // Skip '>'
        generic.append('>');
        return new ParseInfo(generic.toString(), index);
    }

    private static ParseInfo parseArrayType(String signature, int index) {
        index++; // Skip '['
        ParseInfo componentType = parseType(signature, index);
        return new ParseInfo(componentType.getValue() + "[]", componentType.getNewIndex());
    }

    public static void main(String[] args) {
//        String signature = "(Ljava/util/Map<Ljava/lang/String;Ljava/util/List<Lcom/mycompany/MyClass;>;>;[Ljava/lang/String;I)V";
//        List<String> parameterTypes = parseParameterTypes(signature);
//        parameterTypes.forEach(System.out::println);

        Map<String, List<String>> signatureMap = new HashMap<>();
        // Test case 1
        signatureMap.put(
                // for signature
                "(Ljava/util/Map<Ljava/lang/String;,Ljava/lang/Object;>;" +
                        "Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)" +
                        "Ljava/util/List<Lcom/rometools/rome/feed/atom/Entry;>;"
                // expected return items
                , Arrays.asList(
                        "java.util.Map<java.lang.String, java.lang.Object>",
                        "jakarta.servlet.http.HttpServletRequest",
                        "jakarta.servlet.http.HttpServletResponse",
                        "java.util.List<com.rometools.rome.feed.atom.Entry>"
                )
        );
        // Test case 2
        signatureMap.put(
                // for signature
                "(JJ)V",
                // expected return items
                Arrays.asList(
                        "J",
                        "J",
                        "V"
                )
        );

        // Test case 3
        signatureMap.put(
                // for signature
                "([J[[J)[V",
                // expected return items
                Arrays.asList(
                        "J[]",
                        "J[][]",
                        "V[]"
                )
        );


        // Test case 4
        signatureMap.put(
                // for signature
                "(Ljava/util/List<Lorg/unlogged/demo/models/CustomerProfile;>;Z)" +
                        "Ljava/util/List<Lorg/unlogged/demo/models/CustomerProfile;>;",
                // expected return items
                Arrays.asList(
                        "java.util.List<org.unlogged.demo.models.CustomerProfile>",
                        "Z",
                        "java.util.List<org.unlogged.demo.models.CustomerProfile>"
                )
        );

        // Test case 5
        signatureMap.put(
                // for signature
                "()Lorg/springframework/data/redis/core/ReactiveRedisOperations<Ljava/lang/String;,Lorg/unlogged/demo/models/Student;>;",
                // expected return items
                Arrays.asList(
                        "org.springframework.data.redis.core.ReactiveRedisOperations<java.lang.String,org.unlogged.demo.models.Student>"
                )
        );


        for (String signature : signatureMap.keySet()) {
            List<String> parameterTypes1 = parseMethodSignature(signature);
            parameterTypes1.forEach(System.out::println);
            List<String> expectedResult = signatureMap.get(signature);
            for (int i = 0; i < expectedResult.size(); i++) {
                String expected = expectedResult.get(i);
                String actual = parameterTypes1.get(i);
                if (!Objects.equals(expected, actual)) {
                    System.err.println("fail: expected [" + expected + "] vs actual [" + actual + "] at " + signature);
                }

            }

        }


    }

    static private class ParseInfo {
        String value;
        int newIndex;

        public ParseInfo(String value, int newIndex) {
            this.value = value;
            this.newIndex = newIndex;
        }

        public String getValue() {
            return value;
        }

        public int getNewIndex() {
            return newIndex;
        }
    }
}