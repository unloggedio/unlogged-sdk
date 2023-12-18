package io.unlogged.core.bytecode;

import java.util.HashMap;

import org.objectweb.asm.*;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String className;
	private String desc;
	private String nameProbed;
	private int classCounter;
	private int defaultCounter = 10;
	private HashMap<String, Integer> methodCounter = new HashMap<String, Integer>();

	public MethodVisitorWithoutProbe(int api, String methodName, String className, String desc, int classCounter, MethodVisitor mv) {
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

	private int getDivisor(){
		int divisor = this.defaultCounter;

		// write classCounter if defined 
		if (this.classCounter != 0) {
			divisor = this.classCounter;
		}

		// write methodCounter if defined
		if (this.methodCounter.get(this.methodName) != null) {
			divisor = this.methodCounter.get(this.methodName);
		}

		return divisor;
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

		// add the if condition
		Label exitLabel = new Label();
		mv.visitFieldInsn(Opcodes.GETSTATIC, "CustomerController", "map_store", "Ljava/util/Map;");
		mv.visitLdcInsn("gen_sum");
		int divisor = getDivisor();
		mv.visitIntInsn(Opcodes.BIPUSH, divisor);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "Runtime", "probeCounter", "(Ljava/util/Map;Ljava/lang/String;I)Z", false);
		mv.visitJumpInsn(Opcodes.IFEQ, exitLabel);
	
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


		// add `if (probeCounter(className.map_store,  methodName, divisor))` condition

		super.visitCode();
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		// check for the annotation @UnloggedParamMethod
		if ("Lio/unlogged/UnloggedParamMethod;".equals(descriptor)) {
			return new AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
				@Override
				public void visit(String key, Object value) {
					// check for key string
					if ("loggingFrequency".equals(key)) {
						Integer valueInteger = Integer.parseInt((String)value);
						methodCounter.put(methodName, valueInteger);
					}
					super.visit(key, value);
				}
			};
		}
		return super.visitAnnotation(descriptor, visible);
	}
}
