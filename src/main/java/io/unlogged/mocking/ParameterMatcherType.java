package io.unlogged.mocking;

public enum ParameterMatcherType {
    EQUAL,
    ANY_OF_TYPE,
    ANY,
    NULL,
    TRUE,
    FALSE,
    NOT_NULL,
    ANY_STRING,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES_REGEX,
    ANY_SHORT,
    ANY_CHAR,
    ANY_FLOAT,
    ANY_DOUBLE,
    ANY_BYTE,
    ANY_BOOLEAN,
    ANY_MAP,
    ANY_SET,
    ANY_LIST;


    @Override
    public String toString() {
        switch (this) {

            case EQUAL:
                return "Equal";
            case ANY_OF_TYPE:
                return "Any value of";
            case ANY:
                return "Any (everything)";
            case NULL:
                return "Is null";
            case TRUE:
                return "True";
            case FALSE:
                return "False";
            case NOT_NULL:
                return "Is not null";
            case ANY_STRING:
                return "Any String";
            case STARTS_WITH:
                return "String starts with";
            case ENDS_WITH:
                return "String ends with";
            case MATCHES_REGEX:
                return "String matches regex";
            case ANY_SHORT:
                return "Any short";
            case ANY_CHAR:
                return "Any char";
            case ANY_FLOAT:
                return "Any float";
            case ANY_DOUBLE:
                return "Any double";
            case ANY_BYTE:
                return "Any byte";
            case ANY_BOOLEAN:
                return "Any boolean";
            case ANY_MAP:
                return "Any map";
            case ANY_SET:
                return "Any set";
            case ANY_LIST:
                return "Any list";
        }
        return "Invalid parameter matcher";
    }
}
