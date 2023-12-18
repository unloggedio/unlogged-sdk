package io.unlogged.core.bytecode;
import java.util.HashMap;

import org.objectweb.asm.*;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String className;
	private String desc;
	private String nameProbed;
	private HashMap<String, Integer> classCounter;

	public MethodVisitorWithoutProbe(int api, String methodName, String className, String desc, HashMap<String, Integer> classCounter, MethodVisitor mv) {
		super(api, mv);
		this.methodName = methodName;
		this.className = className;
		this.desc = desc;
		this.classCounter = classCounter;
		this.nameProbed = this.methodName + "_PROBED";
	}

	private void pushArgument(MethodVisitor mv, int argIndex, Type argType) {
        // Determine the opcode based on the argument type
        int opcode;
        if (argType.equals(Type.INT_TYPE) || argType.equals(Type.BOOLEAN_TYPE) || argType.equals(Type.CHAR_TYPE) || argType.equals(Type.SHORT_TYPE) || argType.equals(Type.BYTE_TYPE)) {
            opcode = Opcodes.ILOAD;
        } else if (argType.equals(Type.FLOAT_TYPE)) {
            opcode = Opcodes.FLOAD;
        } else if (argType.equals(Type.LONG_TYPE)) {
            opcode = Opcodes.LLOAD;
        } else if (argType.equals(Type.DOUBLE_TYPE)) {
            opcode = Opcodes.DLOAD;
        } else {
            opcode = Opcodes.ALOAD;
        }

        // Load the argument onto the stack
        mv.visitVarInsn(opcode, argIndex);

        // If the argument type is double or long, increment the index again
        if (argType.equals(Type.LONG_TYPE) || argType.equals(Type.DOUBLE_TYPE)) {
            argIndex++;
        }
    }

	@Override
	public void visitCode() {
		// Add the line: this.map_store.put(this.method_name, map_store.get(method_name) + 1);
		mv.visitFieldInsn(Opcodes.GETSTATIC, this.className, "map_store", "Ljava/util/Map;");
		mv.visitLdcInsn(this.methodName);

		mv.visitFieldInsn(Opcodes.GETSTATIC, this.className, "map_store", "Ljava/util/Map;"); // Load map_store onto the stack
		mv.visitLdcInsn(this.methodName);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		mv.visitInsn(Opcodes.ICONST_1); // Load the constant 1 onto the stack
		mv.visitInsn(Opcodes.IADD); // Add the values
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
		// Pop the result (discard it)
		mv.visitInsn(Opcodes.POP);
	

		// add the if logic
		Label exitLabel = new Label();

		// get counter LHS value 
		mv.visitFieldInsn(Opcodes.GETSTATIC, this.className, "map_store", "Ljava/util/Map;");
		
		mv.visitLdcInsn(this.methodName);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);

		// Compute the logging condition and jump when reminder is not zero 
		// divisor for frequency logging
		int divisor = 10; // default value

		if (this.classCounter.get(className) != null) {
			divisor = this.classCounter.get(className);
		}

		mv.visitLdcInsn(divisor);
		mv.visitInsn(Opcodes.IREM);
		mv.visitJumpInsn(Opcodes.IFNE, exitLabel);
	
		// add data to stack
		Type[] argumentTypes = Type.getArgumentTypes(this.desc);
		for (int i = 0; i <= argumentTypes.length-1; i++) {
			pushArgument(mv, i + 1, argumentTypes[i]);
		}

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "this", this.nameProbed, this.desc, false);
		// Return the result from the method
		mv.visitInsn(Opcodes.IRETURN);

		// Exit label
		mv.visitLabel(exitLabel);

		super.visitCode();
	}
}
