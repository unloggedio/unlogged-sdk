package io.unlogged.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTypeUtil {

    public ClassTypeUtil() {
    }

//    public static List<String> splitMethodDesc(String desc) {
//        int beginIndex = desc.indexOf('(');
//        int endIndex = desc.lastIndexOf(')');
//        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
//            System.err.println(beginIndex);
//            System.err.println(endIndex);
//            throw new RuntimeException();
//        }
//        String x0;
//        if (beginIndex == -1 && endIndex == -1) {
//            x0 = desc;
//        } else {
//            x0 = desc.substring(beginIndex + 1, endIndex);
//        }
//        Pattern pattern = Pattern.compile(
//                "\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
//        Matcher matcher = pattern.matcher(x0);
//        List<String> listMatches = new LinkedList<>();
//        while (matcher.find()) {
//            listMatches.add(matcher.group());
//        }
//        listMatches.add(desc.substring(endIndex + 1));
//        return listMatches;
//    }


    public static JavaType getClassNameFromDescriptor(String descriptor, TypeFactory typeFactory) {
//        System.err.println("Get class for: [" + descriptor + "]");
//        typeFactory.constructSimpleType(void.class, null);
        if (descriptor.endsWith("[]")) {
            return typeFactory.constructArrayType(
                    getClassNameFromDescriptor(descriptor.substring(0, descriptor.length() - 2), typeFactory)
            );
        }
        char firstChar = descriptor.charAt(0);
        switch (firstChar) {
            case 'V':
                return typeFactory.constructSimpleType(void.class, null);
            case 'Z':
                return typeFactory.constructSimpleType(boolean.class, null);
            case 'B':
                return typeFactory.constructSimpleType(byte.class, null);
            case 'C':
                return typeFactory.constructSimpleType(char.class, null);
            case 'S':
                return typeFactory.constructSimpleType(short.class, null);
            case 'I':
                return typeFactory.constructSimpleType(int.class, null);
            case 'J':
                return typeFactory.constructSimpleType(long.class, null);
            case 'F':
                return typeFactory.constructSimpleType(float.class, null);
            case 'D':
                return typeFactory.constructSimpleType(double.class, null);
            case 'L':
                String className = descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                return typeFactory.constructFromCanonical(className);
            case '[':
                String componentClassName = descriptor.substring(1);
                return typeFactory.constructArrayType(getClassNameFromDescriptor(componentClassName, typeFactory));
        }
        if (descriptor.startsWith("byte")) {
            return typeFactory.constructSimpleType(byte.class, null);
        }
        if (descriptor.startsWith("boolean")) {
            return typeFactory.constructSimpleType(boolean.class, null);
        }
        if (descriptor.startsWith("long")) {
            return typeFactory.constructSimpleType(long.class, null);
        }
        if (descriptor.startsWith("float")) {
            return typeFactory.constructSimpleType(float.class, null);
        }
        if (descriptor.startsWith("short")) {
            return typeFactory.constructSimpleType(short.class, null);
        }
        if (descriptor.startsWith("int")) {
            return typeFactory.constructSimpleType(int.class, null);
        }
        if (descriptor.startsWith("double")) {
            return typeFactory.constructSimpleType(double.class, null);
        }
        if (descriptor.startsWith("void")) {
            return typeFactory.constructSimpleType(void.class, null);
        }
        if (descriptor.startsWith("char")) {
            return typeFactory.constructSimpleType(char.class, null);
        }
        return typeFactory.constructFromCanonical(descriptor);

    }
}
