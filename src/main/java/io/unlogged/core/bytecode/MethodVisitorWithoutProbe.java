package io.unlogged.core.bytecode;

import java.util.HashMap;

import org.objectweb.asm.*;

import io.unlogged.UnloggedLoggingLevel;
import io.unlogged.UnloggedMode;
import io.unlogged.core.processor.UnloggedProcessorConfig;
import io.unlogged.util.ClassTypeUtil;
import io.unlogged.util.DistinctClassLogNameMap;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String fullClassName;
	private String desc;
	private String methodCompoundName;
	private String nameProbed;
	private long classCounter;
	private HashMap<String, Long> methodCounter = new HashMap<String, Long>();
	private int access;
	private UnloggedProcessorConfig unloggedProcessorConfig;
	private Boolean isStatic;

	public MethodVisitorWithoutProbe(int api, String methodName, String nameProbed, String fullClassName, int access, String desc, long classCounter, MethodVisitor mv, UnloggedProcessorConfig unloggedProcessorConfig) {
		super(api, mv);
		this.methodName = methodName;
		this.fullClassName = fullClassName;
		this.access = access;
		this.desc = desc;
		this.classCounter = classCounter;
		this.nameProbed = nameProbed;
		this.unloggedProcessorConfig = unloggedProcessorConfig;
		this.methodCompoundName = DistinctClassLogNameMap.getMethodCompoundName(fullClassName, methodName, desc);
		this.isStatic = ((this.access & Opcodes.ACC_STATIC) != 0);
	}

	private void pushArgument(MethodVisitor mv, boolean boxing) {

		Type[] argumentTypes = Type.getArgumentTypes(this.desc);

		// For static methods the arguments start from zeorth position and
		// for dynamic methods "this" keyword is placed in zeroth position and other args are placed from first position
		int argIndex;
		if (this.isStatic) {
			argIndex = 0;
		}
		else {
			argIndex = 1;
		}

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
		long divisor = 1;
		if (this.unloggedProcessorConfig.getUnloggedMode() == UnloggedMode.All) {
			divisor = this.unloggedProcessorConfig.getDefaultCounter();
		}
		else if (this.unloggedProcessorConfig.getUnloggedMode() == UnloggedMode.LogAnnotatedOnly) {
			divisor = -1;
		}

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

	private static int getReturnOpcode(String descriptor) {
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
	public void visitCode() {
		long divisor = getDivisor();
		if (divisor != -1) {
			// add the if condition
			Label exitLabel = new Label();

			// load methodName and divisor
			mv.visitLdcInsn(this.methodCompoundName);
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
			String probeCounterDesc = "(Ljava/lang/String;J" + descParsedString + ")Z";
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/Runtime", "probeCounter", probeCounterDesc, false);

			// add the exit jump
			mv.visitJumpInsn(Opcodes.IFEQ, exitLabel);

			// call the line for invoking the probed method
			if (isStatic) {
				pushArgument(mv, false);
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
		}

		super.visitCode();
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
