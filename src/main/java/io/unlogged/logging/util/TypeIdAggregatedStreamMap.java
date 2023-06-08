package io.unlogged.logging.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * This class is to assign an ID for each data type.
 * We use an ID instead of a type name (String) because
 * Java VM may load multiple versions of the same class.
 * In other words, a number of classes may have the same type name.
 */
public class TypeIdAggregatedStreamMap {

    /**
     * A type ID for a constant null.
     */
    public static final int TYPEID_NULL = -1;
    public static final int TYPEID_VOID = 0;
    public static final int TYPEID_BOOLEAN = 1;
    public static final int TYPEID_BYTE = 2;
    public static final int TYPEID_CHAR = 3;
    public static final int TYPEID_DOUBLE = 4;
    public static final int TYPEID_FLOAT = 5;
    public static final int TYPEID_INT = 6;
    public static final int TYPEID_LONG = 7;
    public static final int TYPEID_SHORT = 8;
    public static final int TYPEID_OBJECT = 9;

    /**
     * For convenience The order is the same as constants TYPE_ID_*.
     * A constant TYPEID_STRING does not exist because it is unnecessary.
     */
    private static final Class<?>[] BASIC_TYPE_CLASS = {
            void.class, boolean.class, byte.class, char.class, double.class,
            float.class, int.class, long.class, short.class, Object.class, String.class
    };
    /**
     * A list of type information.
     * The index is the same as typeId.
     */
    private static final String SEPARATOR = ",";
    private final AggregatedFileLogger aggregatedLogger;
    /**
     * Mapping from a Class object to String type ID.
     */
    private final HashMap<Class<?>, Integer> classToIdMap;
    //    private final IEventLogger eventLogger;
    private int nextId;

    /**
     * Create an initial map containing only basic types.
     *
     * @param aggregatedLogger destination aggregated logger
     */
    public TypeIdAggregatedStreamMap(AggregatedFileLogger aggregatedLogger) {
        this.aggregatedLogger = aggregatedLogger;
        classToIdMap = new HashMap<>(65536);
        for (int i = 0; i < BASIC_TYPE_CLASS.length; ++i) {
            Integer id = createTypeRecord(BASIC_TYPE_CLASS[i]);
            assert i >= 10 || id.equals(i);
        }
    }

    /**
     * @param typeId specifies a type.
     * @return true if the ID represents a primitive type, void, or null.
     * @deprecated This method is unused in the current version.
     */
    public static boolean isPrimitiveTypeOrNull(final int typeId) {
        switch (typeId) {
            case TypeIdAggregatedStreamMap.TYPEID_VOID:
            case TypeIdAggregatedStreamMap.TYPEID_BYTE:
            case TypeIdAggregatedStreamMap.TYPEID_CHAR:
            case TypeIdAggregatedStreamMap.TYPEID_DOUBLE:
            case TypeIdAggregatedStreamMap.TYPEID_FLOAT:
            case TypeIdAggregatedStreamMap.TYPEID_INT:
            case TypeIdAggregatedStreamMap.TYPEID_LONG:
            case TypeIdAggregatedStreamMap.TYPEID_SHORT:
            case TypeIdAggregatedStreamMap.TYPEID_BOOLEAN:
            case TypeIdAggregatedStreamMap.TYPEID_NULL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Assign an ID to a type.
     *
     * @param type specifies a type to be translated into an ID.
     * @return a String of an integer ID (to be stored in a HashMap).
     */
    private int createTypeRecord(Class<?> type) {
        // Assign type IDs to dependent classes first.
//        System.out.println("Create type record: " + type.getCanonicalName());
        int superClass = getTypeIdString(type.getSuperclass());
        int componentType = getTypeIdString(type.getComponentType());

        List<Integer> interfaceClasses = new LinkedList<>();
        for (Class<?> anInterface : type.getInterfaces()) {
            int interfaceClassId = getTypeIdString(anInterface);
//                System.err.println(type.getName() + " has interface " + anInterface.getName() +
//                        " -> " + interfaceClassId);
            interfaceClasses.add(interfaceClassId);
        }


        // Getting a class location may load other types (if a custom class loader is working with selogger)
        String classLocation = getClassLocation(type);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream record = new DataOutputStream(byteArrayOutputStream);
        String typeNameFromClass = getTypeNameFromClass(type);
        String classLoaderIdentifier = TypeIdUtil.getClassLoaderIdentifier(type.getClassLoader(), type.getName());


        int newId = nextId++;
        classToIdMap.put(type, newId);

        try {
            record.writeInt(newId);
            record.writeInt(typeNameFromClass.getBytes().length);
            record.write(typeNameFromClass.getBytes());

            record.writeInt(classLocation.getBytes().length);
            record.write(classLocation.getBytes());

            record.writeInt(superClass);
            record.writeInt(componentType);

            record.writeInt(classLoaderIdentifier.getBytes().length);
            record.write(classLoaderIdentifier.getBytes());
            record.writeInt(interfaceClasses.size());

            for (Integer interfaceClass : interfaceClasses) {
                record.writeInt(interfaceClass);
            }


        } catch (IOException e) {
            /// should never happen
        }

        aggregatedLogger.writeNewTypeRecord(newId, typeNameFromClass, byteArrayOutputStream.toByteArray());
        return newId;
    }

    /**
     * Return a string representing a type ID number.
     * This is to generate a type ID list file.
     *
     * @param type Class type for which to create an Id
     * @return a new id for this type if it didnt exist already, else the original one
     */
    public int getTypeIdString(Class<?> type) {
        if (type == null) {
            return TYPEID_NULL;
        } else {
            if (classToIdMap.containsKey(type)) {
//                System.out.println("Class already exists: " + type.getCanonicalName());
                return classToIdMap.get(type);
            }
            return createTypeRecord(type);
        }
    }

    /**
     * Extract a readable type name for a given class.
     *
     * @param type specifies a type.
     * @return a string.
     * For an array, it returns "int[]" instead of "[I".
     */
    private String getTypeNameFromClass(Class<?> type) {
        if (type.isArray()) {
            int count = 0;
            while (type.isArray()) {
                count++;
                type = type.getComponentType();
            }
            StringBuilder b = new StringBuilder(type.getName().length() + count * 2);
            b.append(type.getName());
            for (int i = 0; i < count; ++i) {
                b.append("[]");
            }
            return b.toString();
        } else {
            return type.getName();
        }
    }

    /**
     * Obtain a string where a class is loaded from.
     * The original version is found at http://stackoverflow.com/questions/227486/find-where-java-class-is-loaded-from/19494116#19494116
     * getCanonicalName() is replaced with getTypeNmae() in order to return the correct result for inner classes.
     */
    private String getClassLocation(Class<?> c) {
        ClassLoader loader = c.getClassLoader();
        if (loader == null) {
            // Try the bootstrap class loader - obtained from the ultimate parent of the System Class Loader.
            loader = ClassLoader.getSystemClassLoader();
            while (loader != null && loader.getParent() != null) {
                loader = loader.getParent();
            }
        }
        if (loader != null) {
            String name = c.getName();
            if (name != null) {
                URL resource = loader.getResource(name.replace(".", "/") + ".class");
                if (resource != null) {
                    return resource.toString();
                }
            }
        }
        return "";
    }

}
