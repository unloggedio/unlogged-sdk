package io.unlogged.core.bytecode;

import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.*;

import io.unlogged.core.processor.UnloggedProcessor;
import io.unlogged.util.ClassTypeUtil;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String fullClassName;
	private String desc;
	private String nameProbed;
	private int classCounter;
	private int defaultCounter;
	private HashMap<String, Integer> methodCounter = new HashMap<String, Integer>();
	private int access;

	public MethodVisitorWithoutProbe(int api, String methodName, String fullClassName, int access, String desc, int classCounter, MethodVisitor mv) {
		super(api, mv);
		this.methodName = methodName;
		this.fullClassName = fullClassName;
		this.access = access;
		this.desc = desc;
		this.classCounter = classCounter;
		this.nameProbed = this.methodName + "_PROBED";
		this.defaultCounter = UnloggedProcessor.getDefaultCounter();
	}

	private void pushArgument(MethodVisitor mvs) {

		Type[] argumentTypes = Type.getArgumentTypes(this.desc);
		for (int i = 0; i <= argumentTypes.length-1; i++) {
			
			int argIndex = i+1;
			Type argType = argumentTypes[i];
			
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
    }

	// TODO: change divisor to long 
	private long getDivisor(){
		long divisor = this.defaultCounter;

		// write classCounter if defined 
		if (this.classCounter != 0) {
			divisor = (long)this.classCounter;
		}

		// write methodCounter if defined
		if (this.methodCounter.get(this.methodName) != null) {
			divisor = (long)this.methodCounter.get(this.methodName);
		}

		return divisor;
	}

	@Override
	public void visitCode() {
		// Start of block-A. This adds the line: map_store.put(method_name, map_store.get(method_name) + 1);

		// load map_store
		mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			"map_store",
			"Ljava/util/HashMap;"
		);

		// load string for map_store
		mv.visitLdcInsn(this.methodName);

		// Start of block-B. This adds the logic for  map_store.get("method_name") + 1 and loads the value to stack
		// load map_store
        mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			"map_store",
			"Ljava/util/HashMap;"
        );
		
		// load method name 
		mv.visitLdcInsn(this.methodName);

		// invoke get of map_store for method name
        mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(java.util.HashMap.class),
			"get",
			"(Ljava/lang/Object;)Ljava/lang/Object;",
			false
        );

		// cast long_object to long_primitive
		mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Long.class));
		mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(Long.class),
			"longValue",
			"()J",
			false
		);


		// increment the map_store counter 
		mv.visitLdcInsn(1L);
		mv.visitInsn(Opcodes.LADD);
		// End of Block-B

		// cast long_primitive to long_object
		mv.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			Type.getInternalName(Long.class),
			"valueOf",
			"(J)Ljava/lang/Long;",
			false
		);
		
		// call the put method
		mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(java.util.HashMap.class),
			"put",
			"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
			false
		);

		// Pop the result (discard it)
		mv.visitInsn(Opcodes.POP);
		// End of Block-A

		// Start of block-C
		// This adds the line for if (probecounter)
		// add the if condition
		Label exitLabel = new Label();

		// load the map
		mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			"map_store",
			"Ljava/util/HashMap;"
        );

		// load the method name 
		mv.visitLdcInsn(this.methodName);

		// this is logic for: map_store.get("method_name") + 1
        mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(java.util.HashMap.class),
			"get",
			"(Ljava/lang/Object;)Ljava/lang/Object;",
			false
        );

		// cast long_object to long_primitive
		mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Long.class));
		mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(Long.class),
			"longValue",
			"()J",
			false
		);

		// resolve the value of divisor for frequency logging
		long divisor = getDivisor();
		mv.visitLdcInsn(divisor);

		// load arguments of method in stack and define the desc of calling method
		pushArgument(mv);
		List<String> descParsed = ClassTypeUtil.splitMethodDesc(this.desc);
		String descParsedString = "";
		for (int i=0;i<=descParsed.size()-2;i++) {
			descParsedString = descParsedString + descParsed.get(i);
		}
		String probeCounterDesc = "(JJ" + descParsedString + ")Z";
		
		// call the probeCounter method
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/Runtime", "probeCounter", probeCounterDesc, false);
		
		// add the exit jump
		mv.visitJumpInsn(Opcodes.IFEQ, exitLabel);

		// call the line for invoking the unprobed method
		boolean isStatic = (this.access & Opcodes.ACC_STATIC) != 0;
		if (isStatic) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.fullClassName, this.nameProbed, this.desc, false);
		}
		else{
			visitVarInsn(Opcodes.ALOAD, 0);
			pushArgument(mv);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, this.fullClassName, this.nameProbed, this.desc, false);
		}
		
		// Return the result from the method
		int returnOpcode = getReturnOpcode(this.desc);
		mv.visitInsn(returnOpcode);

		// Exit label
		mv.visitLabel(exitLabel);		
		super.visitCode();
	}

	public static int getReturnOpcode(String descriptor) {
        Type returnType = Type.getReturnType(descriptor);

        if (returnType.getSort() == Type.VOID) {
            throw new IllegalArgumentException("Descriptor represents a void return type");
        } else if (returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY) {
            return Opcodes.ARETURN;
        } else if (returnType.getSort() == Type.BOOLEAN
                || returnType.getSort() == Type.CHAR
                || returnType.getSort() == Type.BYTE
                || returnType.getSort() == Type.SHORT
                || returnType.getSort() == Type.INT) {
            return Opcodes.IRETURN;
        } else if (returnType.getSort() == Type.FLOAT) {
            return Opcodes.FRETURN;
        } else if (returnType.getSort() == Type.LONG) {
            return Opcodes.LRETURN;
        } else if (returnType.getSort() == Type.DOUBLE) {
            return Opcodes.DRETURN;
        } else {
            throw new IllegalArgumentException("Unsupported return type: " + returnType.getClassName());
        }
    }

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		// check for the annotation @UnloggedMethod
		if ("Lio/unlogged/UnloggedMethod;".equals(descriptor)) {
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
