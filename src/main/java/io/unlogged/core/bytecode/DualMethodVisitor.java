package io.unlogged.core.bytecode;

import org.objectweb.asm.*;

public class DualMethodVisitor extends MethodVisitor {

	private final MethodVisitorWithoutProbe methodVisitorWithoutProbe;
	private final MethodVisitor methodVisitorProbed;

	public DualMethodVisitor(MethodVisitorWithoutProbe methodVisitorWithoutProbe, MethodVisitor methodVisitorProbed) {
		super(Opcodes.ASM7);
		this.methodVisitorWithoutProbe = methodVisitorWithoutProbe;
		this.methodVisitorProbed = methodVisitorProbed;
	}

	/*
	 * unimplemented method: getDelegate
	 */

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		if (methodVisitorWithoutProbe != null) {
			return methodVisitorWithoutProbe.visitAnnotationDefault();
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
			return methodVisitorWithoutProbe.visitAnnotation(descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
		return methodVisitorWithoutProbe.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(
    final int parameter, final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
		return methodVisitorWithoutProbe.visitParameterAnnotation(parameter, descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(
    final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
		return methodVisitorWithoutProbe.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(
	final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
		return methodVisitorWithoutProbe.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(
		final int typeRef,
		final TypePath typePath,
		final Label[] start,
		final Label[] end,
		final int[] index,
		final String descriptor,
		final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
		return methodVisitorWithoutProbe.visitLocalVariableAnnotation(
		typeRef, typePath, start, end, index, descriptor, visible);
		}
		else {
			return null;
		}
	}

	@Override
	public void visitParameter(final String name, final int access) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitParameter(name, access);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitParameter(name, access);
		}
	}

	@Override
	public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitAnnotableParameterCount(parameterCount, visible);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitAnnotableParameterCount(parameterCount, visible);
		}
	}

	@Override
	public void visitAttribute(final Attribute attribute) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitAttribute(attribute);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitAttribute(attribute);
		}
	}

	@Override
	public void visitCode() {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitCode();
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitCode();
		}
	}

	@Override
	public void visitFrame(
		final int type,
		final int numLocal,
		final Object[] local,
		final int numStack,
		final Object[] stack) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitFrame(type, numLocal, local, numStack, stack);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitFrame(type, numLocal, local, numStack, stack);
		}
	}

	@Override
	public void visitInsn(final int opcode) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitInsn(opcode);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitInsn(opcode);
		}
	}

	@Override
	public void visitIntInsn(final int opcode, final int operand) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitIntInsn(opcode, operand);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitIntInsn(opcode, operand);
		}
	}

	@Override
	public void visitVarInsn(final int opcode, final int varIndex) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitVarInsn(opcode, varIndex);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitVarInsn(opcode, varIndex);
		}
	}

	@Override
	public void visitTypeInsn(final int opcode, final String type) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitTypeInsn(opcode, type);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitTypeInsn(opcode, type);
		}
	}

	@Override
	public void visitFieldInsn(
		final int opcode, final String owner, final String name, final String descriptor) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitFieldInsn(opcode, owner, name, descriptor);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitFieldInsn(opcode, owner, name, descriptor);
		}
	}

	@Override
	public void visitMethodInsn(
		final int opcode,
		final String owner,
		final String name,
		final String descriptor,
		final boolean isInterface) {
		
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
		}
	}

	@Override
	public void visitInvokeDynamicInsn(
		final String name,
		final String descriptor,
		final Handle bootstrapMethodHandle,
		final Object... bootstrapMethodArguments) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		}
		if (methodVisitorWithoutProbe!= null) {
			methodVisitorWithoutProbe.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		}
	}

	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitJumpInsn(opcode, label);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitJumpInsn(opcode, label);
		}
	}

	@Override
	public void visitLabel(final Label label) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitLabel(label);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitLabel(label);
		}
	}

	@Override
	public void visitLdcInsn(final Object value) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitLdcInsn(value);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitLdcInsn(value);
		}
	}

	@Override
	public void visitIincInsn(final int varIndex, final int increment) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitIincInsn(varIndex, increment);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitIincInsn(varIndex, increment);
		}
	}

	@Override
	public void visitTableSwitchInsn(
		final int min, final int max, final Label dflt, final Label... labels) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitTableSwitchInsn(min, max, dflt, labels);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitTableSwitchInsn(min, max, dflt, labels);
		}
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitLookupSwitchInsn(dflt, keys, labels);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitLookupSwitchInsn(dflt, keys, labels);
		}
	}

	@Override
	public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitMultiANewArrayInsn(descriptor, numDimensions);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitMultiANewArrayInsn(descriptor, numDimensions);
		}
	}

	@Override
	public void visitTryCatchBlock(
		final Label start, final Label end, final Label handler, final String type) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitTryCatchBlock(start, end, handler, type);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitTryCatchBlock(start, end, handler, type);
		}
	}

	@Override
	public void visitLocalVariable(
		final String name,
		final String descriptor,
		final String signature,
		final Label start,
		final Label end,
		final int index) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitLocalVariable(name, descriptor, signature, start, end, index);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitLocalVariable(name, descriptor, signature, start, end, index);
		}
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitLineNumber(line, start);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitLineNumber(line, start);
		}
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitMaxs(maxStack, maxLocals);
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitMaxs(maxStack, maxLocals);
		}
	}

	@Override
	public void visitEnd() {
		if (methodVisitorProbed != null) {
			methodVisitorProbed.visitEnd();
		}
		if (methodVisitorWithoutProbe != null) {
			methodVisitorWithoutProbe.visitEnd();
		}
 	}
}
