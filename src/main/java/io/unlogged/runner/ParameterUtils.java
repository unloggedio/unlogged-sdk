package io.unlogged.runner;

public class ParameterUtils {

    public static String getFloatValue(String input) {
        try {
            return String.valueOf(Float.intBitsToFloat(Integer.parseInt(input)));
        } catch (Exception e) {
            return input;
        }
    }

    public static String getDoubleValue(String input) {
        try {
            return String.valueOf(Double.longBitsToDouble(Long.parseLong(input)));
        } catch (Exception e) {
            return input;
        }
    }

    public static String processResponseForFloatAndDoubleTypes(String responseClassname, String stringValue) {
        if (responseClassname == null) {
            return stringValue;
        }
        if (responseClassname.equalsIgnoreCase("float")
                || responseClassname.equalsIgnoreCase("java.lang.float")
                || responseClassname.equalsIgnoreCase("Ljava/lang/Float;")
                || responseClassname.equals("F")) {
            return getFloatValue(stringValue);
        }
        if (responseClassname.equalsIgnoreCase("double")
                || responseClassname.equalsIgnoreCase("java.lang.double")
                || responseClassname.equalsIgnoreCase("Ljava/lang/Double;")
                || responseClassname.equals("D")) {
            return getDoubleValue(stringValue);
        }
        return stringValue;
    }
}