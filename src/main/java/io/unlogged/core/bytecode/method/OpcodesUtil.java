package io.unlogged.core.bytecode.method;

import com.insidious.common.weaver.Descriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This is a utility class providing static methods to support bytecode manipulation.
 */
public class OpcodesUtil {

    private static final String[] opcodeNames = new String[]{
            "NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0",
            "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4",
            "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0",
            "FCONST_1", "FCONST_2", "DCONST_0", "DCONST_1",
            "BIPUSH", "SIPUSH", "LDC", "LDC_W",
            "LDC2_W", "ILOAD", "LLOAD", "FLOAD",
            "DLOAD", "ALOAD", "ILOAD_0", "ILOAD_1",
            "ILOAD_2", "ILOAD_3", "LLOAD_0", "LLOAD_1",
            "LLOAD_2", "LLOAD_3", "FLOAD_0", "FLOAD_1",
            "FLOAD_2", "FLOAD_3", "DLOAD_0", "DLOAD_1",
            "DLOAD_2", "DLOAD_3", "ALOAD_0", "ALOAD_1",
            "ALOAD_2", "ALOAD_3", "IALOAD", "LALOAD",
            "FALOAD", "DALOAD", "AALOAD", "BALOAD",
            "CALOAD", "SALOAD", "ISTORE", "LSTORE",
            "FSTORE", "DSTORE", "ASTORE", "ISTORE_0",
            "ISTORE_1", "ISTORE_2", "ISTORE_3", "LSTORE_0",
            "LSTORE_1", "LSTORE_2", "LSTORE_3", "FSTORE_0",
            "FSTORE_1", "FSTORE_2", "FSTORE_3", "DSTORE_0",
            "DSTORE_1", "DSTORE_2", "DSTORE_3", "ASTORE_0",
            "ASTORE_1", "ASTORE_2", "ASTORE_3", "IASTORE",
            "LASTORE", "FASTORE", "DASTORE", "AASTORE",
            "BASTORE", "CASTORE", "SASTORE", "POP",
            "POP2", "DUP", "DUP_X1", "DUP_X2",
            "DUP2", "DUP2_X1", "DUP_X2", "SWAP",
            "IADD", "LADD", "FADD", "DADD",
            "ISUB", "LSUB", "FSUB", "DSUB",
            "IMUL", "LMUL", "FMUL", "DMUL",
            "IDIV", "LDIV", "FDIV", "DDIV",
            "IREM", "LREM", "FREM", "DREM",
            "INEG", "LNEG", "FNEG", "DNEG",
            "ISHL", "LSHL", "ISHR", "LSHR",
            "IUSHR", "LUSHR", "IAND", "LAND",
            "IOR", "LOR", "IXOR", "LXOR",
            "IINC", "I2L", "I2F", "I2D",
            "L2I", "L2F", "L2D", "F2I",
            "F2L", "F2D", "D2I", "D2L",
            "D2F", "I2B", "I2C", "I2S",
            "LCMP", "FCMPL", "FCMPG", "DCMPL",
            "DCMPG", "IFEQ", "IFNE", "IFLT",
            "IFGE", "IFGT", "IFLE", "IF_ICMPEQ",
            "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT",
            "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO",
            "JSR", "RET", "TABLESWITCH", "LOOKUPSWITCH",
            "IRETURN", "LRETURN", "FRETURN", "DRETURN",
            "ARETURN", "RETURN", "GETSTATIC", "PUTSTATIC",
            "GETFIELD", "PUTFIELD", "INVOKEVIRTUAL", "INVOKESPECIAL",
            "INVOKESTATIC", "INVOKEINTERFACE", "INVOKEDYNAMIC", "NEW",
            "NEWARRAY", "ANEWARRAY", "ARRAYLENGTH", "ATHROW",
            "CHECKCAST", "INSTANCEOF", "MONITORENTER", "MONITOREXIT",
            "WIDE", "MULTIANEWARRAY", "IFNULL", "IFNONNULL",
            "GOTO_W", "JSR_W"
    };

    /**
     * @param opcode specifies the opcode of an instruction.
     * @return true if the instruction is one of RETURN instructions.
     */
    public static boolean isReturn(int opcode) {
        return (opcode == Opcodes.ARETURN) || (opcode == Opcodes.RETURN) ||
                (opcode == Opcodes.IRETURN) || (opcode == Opcodes.FRETURN) ||
                (opcode == Opcodes.LRETURN) || (opcode == Opcodes.DRETURN);
    }

    /**
     * @param opcode specifies the opcode of an instruction.
     * @return true if the instruction is one of ARRAY LOAD instructions.
     */
    public static boolean isArrayLoad(int opcode) {
        switch (opcode) {
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.DALOAD:
            case Opcodes.FALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.SALOAD:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param opcode specifies the opcode of an instruction.
     * @return true if the instruction is one of ARRAY STORE instructions.
     */
    public static boolean isArrayStore(int opcode) {
        switch (opcode) {
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.DASTORE:
            case Opcodes.FASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.SASTORE:
                return true;
            default:
                return false;
        }
    }

    /**
     * @param desc specifies a value type.
     * @return opcode of DUP or DUP2 instructions appropriate for the value type.
     */
    public static int getDupInstruction(String desc) {
        if (desc.equals("D") || desc.equals("J")) {
            return Opcodes.DUP2;
        } else {
            return Opcodes.DUP;
        }
    }

    /**
     * @param desc specifies a value type.
     * @return opcode of a load instruction appropriate for the value type.
     */
    public static int getLoadInstruction(String desc) {
        switch (desc) {
            case "B":
            case "C":
            case "I":
            case "S":
            case "Z":
                return Opcodes.ILOAD;

            case "D":
                return Opcodes.DLOAD;

            case "F":
                return Opcodes.FLOAD;

            case "J":
                return Opcodes.LLOAD;

            case "Ljava/lang/Object;":
                return Opcodes.ALOAD;

            case "V":
                assert false : "Void is not a data";

            default:
                assert false : "Unknown primitive";
                return Opcodes.NOP;
        }
    }

    /**
     * @param desc specifies a value type.
     * @return a Type object representing the value type.
     */
    public static Type getAsmType(String desc) {
        switch (desc) {
            case "B":
                return Type.BYTE_TYPE;

            case "C":
                return Type.CHAR_TYPE;

            case "I":
                return Type.INT_TYPE;

            case "S":
                return Type.SHORT_TYPE;

            case "Z":
                return Type.BOOLEAN_TYPE;

            case "D":
                return Type.DOUBLE_TYPE;

            case "F":
                return Type.FLOAT_TYPE;

            case "J":
                return Type.LONG_TYPE;

            case "Ljava/lang/Object;":
                return Type.getObjectType("java/lang/Object");

            case "V":
                return Type.VOID_TYPE;

            default:
                assert false : "Unknown primitive";
                return null;
        }

    }

    /**
     * @param desc specifies a value type.
     * @return opcode of a store instruction appropriate for the value type.
     */
    public static int getStoreInstruction(String desc) {
        switch (desc) {
            case "B":
            case "C":
            case "I":
            case "S":
            case "Z":
                return Opcodes.ISTORE;

            case "D":
                return Opcodes.DSTORE;

            case "F":
                return Opcodes.FSTORE;

            case "J":
                return Opcodes.LSTORE;

            case "Ljava/lang/Object;":
                return Opcodes.ASTORE;

            case "V":
                assert false : "Void is not a data";

            default:
                assert false : "Unknown primitive";
                return Opcodes.NOP;
        }
    }

    /**
     * @param type specifies an element type for NEWARRAY instruction.
     * @return the type name corresponding the type code.
     */
    public static String getArrayElementType(int type) {
        switch (type) {
            case Opcodes.T_BOOLEAN:
                return "boolean";
            case Opcodes.T_CHAR:
                return "char";
            case Opcodes.T_FLOAT:
                return "float";
            case Opcodes.T_DOUBLE:
                return "double";
            case Opcodes.T_BYTE:
                return "byte";
            case Opcodes.T_SHORT:
                return "short";
            case Opcodes.T_INT:
                return "int";
            case Opcodes.T_LONG:
                return "long";
            default:
                assert false : "Unknown Array Type";
                return "Unknown";
        }
    }

    /**
     * @param opcode specifies one of ARRAY STORE instructions.
     * @return a descriptor of the element data type.
     */
    public static String getDescForArrayStore(int opcode) {
        switch (opcode) {
            case Opcodes.BASTORE:
                return "B";
            case Opcodes.CASTORE:
                return "C";
            case Opcodes.DASTORE:
                return "D";
            case Opcodes.FASTORE:
                return "F";
            case Opcodes.IASTORE:
                return "I";
            case Opcodes.LASTORE:
                return "J";
            case Opcodes.SASTORE:
                return "S";
            default:
                assert opcode == Opcodes.AASTORE;
                return "Ljava/lang/Object;";
        }

    }

    /**
     * @param opcode specifies one of ARRAY LOAD instructions.
     * @return a descriptor of the element data type.
     * TODO This method and related methods should go to the Descriptor class
     */
    public static Descriptor getDescForArrayLoad(int opcode) {
        Descriptor elementDesc;
        if (opcode == Opcodes.BALOAD)
            elementDesc = Descriptor.Byte; // Use Object to represent byte[] and boolean[]
        else if (opcode == Opcodes.CALOAD)
            elementDesc = Descriptor.Char;
        else if (opcode == Opcodes.DALOAD)
            elementDesc = Descriptor.Double;
        else if (opcode == Opcodes.FALOAD)
            elementDesc = Descriptor.Float;
        else if (opcode == Opcodes.IALOAD)
            elementDesc = Descriptor.Integer;
        else if (opcode == Opcodes.LALOAD)
            elementDesc = Descriptor.Long;
        else if (opcode == Opcodes.SALOAD)
            elementDesc = Descriptor.Short;
        else {
            assert (opcode == Opcodes.AALOAD);
            elementDesc = Descriptor.Object;
        }
        return elementDesc;
    }

    /**
     * @param opcode specifies one of local store instructions.
     * @return a descriptor of the data type.
     */
    public static Descriptor getDescForStore(int opcode) {
        switch (opcode) {
            case Opcodes.ISTORE:
                return Descriptor.Integer;
            case Opcodes.FSTORE:
                return Descriptor.Float;
            case Opcodes.DSTORE:
                return Descriptor.Double;
            case Opcodes.LSTORE:
                return Descriptor.Long;
            case Opcodes.ASTORE:
                return Descriptor.Object;
            default:
                return null;
        }
    }

    /**
     * @param opcode specifies one of local load instructions.
     * @return a descriptor of the data type.
     */
    public static Descriptor getDescForLoad(int opcode) {
        switch (opcode) {
            case Opcodes.ILOAD:
                return Descriptor.Integer;
            case Opcodes.FLOAD:
                return Descriptor.Float;
            case Opcodes.DLOAD:
                return Descriptor.Double;
            case Opcodes.LLOAD:
                return Descriptor.Long;
            case Opcodes.ALOAD:
                return Descriptor.Object;
            default:
                return null;
        }
    }

    /**
     * @param opcode specifies one of return instructions.
     * @return a descriptor of the data type.
     */
    public static Descriptor getDescForReturn(int opcode) {
        switch (opcode) {
            case Opcodes.IRETURN:
                return Descriptor.Integer;
            case Opcodes.FRETURN:
                return Descriptor.Float;
            case Opcodes.DRETURN:
                return Descriptor.Double;
            case Opcodes.LRETURN:
                return Descriptor.Long;
            case Opcodes.ARETURN:
                return Descriptor.Object;
            case Opcodes.RETURN:
                return Descriptor.Void;
            default:
                return null;
        }
    }

    /**
     * @param opcode specifies a Java bytecode.
     * @return its string name.
     */
    public static String getString(int opcode) {
        if (0 <= opcode && opcode < opcodeNames.length) {
            return opcodeNames[opcode];
        } else {
            return Integer.toString(opcode);
        }
    }


}
