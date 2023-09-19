package io.unlogged.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTypeUtil {

    private ClassTypeUtil() {
    }

    public static List<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if (beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        } else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }
        Pattern pattern = Pattern.compile(
                "\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<String> listMatches = new LinkedList<>();
        while (matcher.find()) {
            listMatches.add(matcher.group());
        }
        listMatches.add(desc.substring(endIndex + 1));
        return listMatches;
    }


    public static Class<?> getClassNameFromDescriptor(String descriptor, ClassLoader targetClassLoader) {
//        System.err.println("Get class for: [" + descriptor + "]");
        char firstChar = descriptor.charAt(0);
        switch (firstChar) {
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;
            case 'B':
                return byte.class;
            case 'C':
                return char.class;
            case 'S':
                return short.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
            case 'L':
                try {
                    return targetClassLoader.loadClass(
                            descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            case '[':
                return java.lang.reflect.Array.newInstance(
                                ClassTypeUtil.getClassNameFromDescriptor(
                                        descriptor.substring(1), targetClassLoader), 0).getClass();
        }
        if (descriptor.startsWith("byte")) {
            return byte.class;
        }
        if (descriptor.startsWith("boolean")) {
            return boolean.class;
        }
        if (descriptor.startsWith("long")) {
            return long.class;
        }
        if (descriptor.startsWith("float")) {
            return float.class;
        }
        if (descriptor.startsWith("short")) {
            return short.class;
        }
        if (descriptor.startsWith("int")) {
            return int.class;
        }
        if (descriptor.startsWith("double")) {
            return double.class;
        }
        if (descriptor.startsWith("void")) {
            return void.class;
        }
        if (descriptor.startsWith("char")) {
            return char.class;
        }
        return null;

    }


}
