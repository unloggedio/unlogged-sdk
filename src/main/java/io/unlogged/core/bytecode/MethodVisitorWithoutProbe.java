package io.unlogged.core.bytecode;

import java.util.HashMap;

import org.objectweb.asm.*;

import io.unlogged.UnloggedLoggingLevel;
import io.unlogged.core.processor.UnloggedProcessorConfig;
import io.unlogged.util.ClassTypeUtil;
import io.unlogged.util.MapStoreName;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String fullClassName;
	private String desc;
	private String nameProbed;
	private long classCounter;
	private HashMap<String, Long> methodCounter = new HashMap<String, Long>();
	private int access;
	private UnloggedProcessorConfig unloggedProcessorConfig;
	private String mapName;

	public MethodVisitorWithoutProbe(int api, String methodName, String nameProbed, String fullClassName, int access, String desc, long classCounter, MethodVisitor mv, UnloggedProcessorConfig unloggedProcessorConfig) {
		super(api, mv);
		this.methodName = methodName;
		this.fullClassName = fullClassName;
		this.mapName = MapStoreName.getClassMapStore(fullClassName);
		this.access = access;
		this.desc = desc;
		this.classCounter = classCounter;
		this.nameProbed = nameProbed;
		this.unloggedProcessorConfig = unloggedProcessorConfig;
	}

	private void pushArgument(MethodVisitor mv, boolean boxing) {

		Type[] argumentTypes = Type.getArgumentTypes(this.desc);
		int argIndex = 1;

		for (int i = 0; i <= argumentTypes.length-1; i++) {
			Type argType = argumentTypes[i];
			int loadOpcode;
			String boxingType;

			// Determine the opcode based on the argument type
			if (argType.equals(Type.INT_TYPE) || argType.equals(Type.BOOLEAN_TYPE) || argType.equals(Type.CHAR_TYPE) || argType.equals(Type.SHORT_TYPE) || argType.equals(Type.BYTE_TYPE)) {
				loadOpcode = Opcodes.ILOAD;
				boxingType = "java/lang/Integer";
			} else if (argType.equals(Type.FLOAT_TYPE)) {
				loadOpcode = Opcodes.FLOAD;
				boxingType = "java/lang/Float";
			} else if (argType.equals(Type.LONG_TYPE)) {
				loadOpcode = Opcodes.LLOAD;
				boxingType = "java/lang/Long";
			} else if (argType.equals(Type.DOUBLE_TYPE)) {
				loadOpcode = Opcodes.DLOAD;
				boxingType = "java/lang/Double";
			} else {
				loadOpcode = Opcodes.ALOAD;
				boxingType = "java/lang/Object";
			}

			// Load the argument onto the stack
			mv.visitVarInsn(loadOpcode, argIndex);

			// Box the primitive type if necessary
			if (boxing && !argType.equals(Type.OBJECT)) {
				String typeDescriptor = argType.getDescriptor();
				String boxingDescriptor = "L" + boxingType + ";";
				mv.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						boxingType,
						"valueOf",
						"(" + typeDescriptor + ")" + boxingDescriptor,
						false
				);
			}
			
			// If the argument type is double or long, increment the index by 2
			if (argType.equals(Type.LONG_TYPE) || argType.equals(Type.DOUBLE_TYPE)) {
				argIndex += 2;
			}
			else {
				argIndex += 1;
			}
		}
    }

	private long getDivisor(){
		long divisor = this.unloggedProcessorConfig.getDefaultCounter();

		// write classCounter if defined 
		if (this.classCounter != (long)0) {
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
		// Start of block-A. This adds the line: mapStore.put(methodName, mapStore.get(methodName) + 1);

		// load mapStore
		mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			this.mapName,
			"Ljava/util/HashMap;"
		);

		// load string for mapStore
		mv.visitLdcInsn(this.methodName);

		// Start of block-B. This adds the logic for  mapStore.get("methodName") + 1 and loads the value to stack
		// load mapStore
        mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			this.mapName,
			"Ljava/util/HashMap;"
        );
		
		// load method name 
		mv.visitLdcInsn(this.methodName);

		// invoke get of mapStore for method name
        mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(java.util.HashMap.class),
			"get",
			"(Ljava/lang/Object;)Ljava/lang/Object;",
			false
        );

		// cast long object to long primitive
		mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Long.class));
		mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(Long.class),
			"longValue",
			"()J",
			false
		);


		// increment the mapStore counter 
		mv.visitLdcInsn(1L);
		mv.visitInsn(Opcodes.LADD);
		// End of Block-B

		// cast long primitive to long object
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
		// This adds the line for if (probecounter(methodCounter, divisor, argument list))
		// add the if condition
		Label exitLabel = new Label();

		// load the map
		mv.visitFieldInsn(
			Opcodes.GETSTATIC,
			this.fullClassName,
			this.mapName,
			"Ljava/util/HashMap;"
        );

		// load the method name 
		mv.visitLdcInsn(this.methodName);

		// this is logic for: mapStore.get("methodName")
        mv.visitMethodInsn(
			Opcodes.INVOKEVIRTUAL,
			Type.getInternalName(java.util.HashMap.class),
			"get",
			"(Ljava/lang/Object;)Ljava/lang/Object;",
			false
        );

		// cast long object to long primitive
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

		String descParsedString = "";
		if (this.unloggedProcessorConfig.getUnloggedLoggingLevel() == UnloggedLoggingLevel.ARGUMENT) {
			// load arguments of method in stack and define the desc of calling method
			pushArgument(mv, true);
			int descSize = ClassTypeUtil.splitMethodDesc(this.desc).size();
			
			for (int i=0;i<=descSize-2;i++) {
				descParsedString = descParsedString + "Ljava/lang/Object;";
			}
		}

		// call the probeCounter method
		String probeCounterDesc = "(JJ" + descParsedString + ")Z";
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/Runtime", "probeCounter", probeCounterDesc, false);
		
		// add the exit jump
		mv.visitJumpInsn(Opcodes.IFEQ, exitLabel);

		// call the line for invoking the probed method
		boolean isStatic = (this.access & Opcodes.ACC_STATIC) != 0;
		if (isStatic) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.fullClassName, this.nameProbed, this.desc, false);
		}
		else{
			visitVarInsn(Opcodes.ALOAD, 0);
			pushArgument(mv, false);
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
			return Opcodes.RETURN;
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
					if ("counter".equals(key)) {
						long valueLong = Long.parseLong((String)value);
						methodCounter.put(methodName, valueLong);
					}
					super.visit(key, value);
				}
			};
		}
		return super.visitAnnotation(descriptor, visible);
	}
}
