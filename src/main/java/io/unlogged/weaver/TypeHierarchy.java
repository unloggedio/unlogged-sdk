package io.unlogged.weaver;


import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TypeHierarchy {

    private final Map<String, String> parentMap = new HashMap<>();
    private final Map<String, String[]> interfaceMap = new HashMap<>();
    private final Names names;
    private final Symtab symtab;
    private Method loadClassMethod = null;
    private ClassFinder classFinder;
    private ClassReader classReader;
    private boolean jvm18 = false;

    public TypeHierarchy(Context context) {
        try {
            Class.forName("com.sun.tools.javac.code.ClassFinder");
            this.classFinder = ClassFinder.instance(context);
        } catch (Exception e) {
            // jvm 1.8
            this.classReader = ClassReader.instance(context);
            try {
                loadClassMethod = classReader.getClass().getMethod("loadClass",
                        Class.forName("com.sun.tools.javac.util.Name"));
                this.jvm18 = true;
            } catch (Exception e1) {
                // what to do
            }
        }
        this.names = Names.instance(context);
        this.symtab = Symtab.instance(context);
    }

    public void registerClass(String className, String superClassName, String[] interfaceNames) {
        if (superClassName != null && superClassName.length() > 0) {
            parentMap.put(className, superClassName);
        }
        if (interfaceNames != null && interfaceNames.length > 0) {
            interfaceMap.put(className, interfaceNames);
        }
    }

//    public String findCommonSuper(String type1, String type2) {
//
//        if (type1.equals(type2)) {
//            return type1;
//        }
//
//        String type1Super = parentMap.get(type1);
//        if (type1Super == null) {
//            type1Super = loadAndRegisterType(type1);
//        }
//        String type2Super = parentMap.get(type2);
//        if (type2Super == null) {
//            type2Super = loadAndRegisterType(type2);
//        }
//
//        if (typeImplements(type2Super, type1)) {
//            return type2Super;
//        }
//
//        if (typeImplements(type1Super, type2)) {
//            return type1Super;
//        }
//
//        return "java/lang/Object";
//    }

    // Need to override in order to overcome class loaders isolation of modern app servers
    // Code is based on a test from an ASM framework and based on behaviour Javassist
    public String getCommonSuperClassWithRealClass(String theType1, String theType2) {
        try {
            Symbol.ClassSymbol info11 = classSymbolForName(theType1);
            Symbol.ClassSymbol info12 = classSymbolForName(theType2);

            if (info11.isInterface()) {
                if (typeImplements(theType2, info12, theType1)) {
                    return theType1;
                }
                if (info12.isInterface()) {
                    if (typeImplements(theType1, info12, theType2)) {
                        return theType2;
                    }
                }
                return "java.lang.Object";
            }

            if (info12.isInterface()) {
                if (typeImplements(theType1, info11, theType2)) {
                    return theType2;
                } else {
                    return "java.lang.Object";
                }
            }

            StringBuilder b1 = typeAncestors(info11);
            StringBuilder b2 = typeAncestors(info12);
            String result = "java.lang.Object";
            int end1 = b1.length();
            int end2 = b2.length();

            while (true) {
                int start1 = b1.lastIndexOf(";", end1 - 1);
                int start2 = b2.lastIndexOf(";", end2 - 1);
                if (start1 != -1 && start2 != -1
                        && end1 - start1 == end2 - start2) {
                    String p1 = b1.substring(start1 + 1, end1);
                    String p2 = b2.substring(start2 + 1, end2);
                    if (p1.equals(p2)) {
                        result = p1;
                        end1 = start1;
                        end2 = start2;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private StringBuilder typeAncestors(Symbol.ClassSymbol theReader) throws IOException {
        StringBuilder b = new StringBuilder();

        while (!"java.lang.Object".equals(theReader.toString())) {
            b.append(';').append(theReader);
            theReader = classSymbolForName(theReader.getSuperclass().toString());
        }

        return b;
    }


    private String loadAndRegisterType(String type1) {
        if ("none".equals(type1)) {
            return type1;
        }
        if (parentMap.containsKey(type1)) {
            return parentMap.get(type1);
        }
        Symbol.ClassSymbol classFromFinder = classSymbolForName(type1);

        List<Type> interfaces = classFromFinder.getInterfaces();
        String[] interfaceNames = new String[interfaces.size()];
        for (int i = 0; i < interfaces.size(); i++) {
            String anInterfaceName = interfaces.get(i).toString();
            interfaceNames[i] = anInterfaceName;
            loadAndRegisterType(anInterfaceName);
        }

        String typeSuper = classFromFinder.getSuperclass().toString();
        registerClass(type1, typeSuper, interfaceNames);
        loadAndRegisterType(typeSuper);
        return typeSuper;
    }

    private Symbol.ClassSymbol classSymbolForName(String type1) {
        if (jvm18) {
            try {
                return (Symbol.ClassSymbol) loadClassMethod.invoke(classReader, names.fromString(type1));
            } catch (Exception e) {
                return null;
            }
//            return classReader.loadClass(names.fromString(type1));
        }
        return classFinder.loadClass(symtab.unnamedModule, names.fromString(type1));
    }

    private boolean typeImplements(String theType, Symbol.ClassSymbol theReader, String theInterface) throws IOException {
        while (!"java.lang.Object".equals(theType)) {
            List<Type> itfs = theReader.getInterfaces();

            for (int i = 0; i < itfs.size(); ++i) {
                if (itfs.get(i).toString().equals(theInterface)) {
                    return true;
                }
            }

            for (int i = 0; i < itfs.size(); ++i) {
                if (typeImplements(itfs.get(i).toString(), classSymbolForName(itfs.get(i).toString()), theInterface)) {
                    return true;
                }
            }

            theType = theReader.getSuperclass().toString();
            if (theType.equals("none")) {
                break;
            }
            theReader = classSymbolForName(theType);
        }

        return false;
    }


}
