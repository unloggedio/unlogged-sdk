package io.unlogged.core.weaver;


import java.util.HashMap;
import java.util.Map;

public class TypeHierarchy {

    final private Map<String, String> parentMap = new HashMap<>();
    final private Map<String, String[]> interfaceMap = new HashMap<>();

    public void registerClass(String className, String superClassName, String[] interfaceNames) {
        if (superClassName != null && superClassName.length() > 0) {
            parentMap.put(className, superClassName);
        }
        if (interfaceNames != null && interfaceNames.length > 0) {
            interfaceMap.put(className, interfaceNames);
        }
    }

    public String findCommonSuper(String type1, String type2) {

        if(type1.equals(type2)) {
            return type1;
        }

        String type1Super = parentMap.get(type1);
        String type2Super = parentMap.get(type2);

        if (typeImplements(type2, type1)) {
            return type2;
        }

        if (typeImplements(type1, type2)) {
            return type1;
        }

        return "java/lang/Object";
    }

    private boolean typeImplements(String type2, String type1) {
        String type1Super = parentMap.get(type1);
        if (type1Super != null) {
            if (type1Super.equals(type2)) {
                return true;
            }
            if (typeImplements(type2, type1Super)) {
                return true;
            }
        }
        String[] type1Interfaces = interfaceMap.get(type1);
        if (type1Interfaces != null) {
            for (String type1Interface : type1Interfaces) {
                if (type1Interface.equals(type2)) {
                    return true;
                }
                if (typeImplements(type2, type1Interface)) {
                    return true;
                }
            }
        }
        return false;
    }

}
