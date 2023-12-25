package io.unlogged.core.bytecode;

import org.objectweb.asm.*;

public class DualMethodVisitor extends MethodVisitor {

	private final MethodVisitor methodVisitorWithoutProbe;
	private final MethodVisitor methodVisitorProbed;

	public DualMethodVisitor(MethodVisitor methodVisitorWithoutProbe, MethodVisitor methodVisitorProbed) {
		super(Opcodes.ASM7);
		this.methodVisitorWithoutProbe = methodVisitorWithoutProbe;
		this.methodVisitorProbed = methodVisitorProbed;
	}

	// Override methods from MethodVisitor and delegate to both mv1 and mv2 as needed

	@Override
	public void visitInsn(int opcode) {
		methodVisitorWithoutProbe.visitInsn(opcode);
		methodVisitorProbed.visitInsn(opcode);
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
	public void visitIntInsn(int opcode, int operand) {
		methodVisitorWithoutProbe.visitIntInsn(opcode, operand);
		methodVisitorProbed.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		methodVisitorWithoutProbe.visitVarInsn(opcode, varIndex);
		methodVisitorProbed.visitVarInsn(opcode, varIndex);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
		if (methodVisitorWithoutProbe != null) {
			return methodVisitorWithoutProbe.visitAnnotation(descriptor, visible);
		}
		return null;
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		methodVisitorWithoutProbe.visitTypeInsn(opcode, type);
		methodVisitorProbed.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		methodVisitorWithoutProbe.visitFieldInsn(opcode, owner, name, descriptor);
		methodVisitorProbed.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		methodVisitorWithoutProbe.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
		methodVisitorProbed.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		methodVisitorWithoutProbe.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		methodVisitorProbed.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		methodVisitorWithoutProbe.visitJumpInsn(opcode, label);
		methodVisitorProbed.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(final Label label) {
		methodVisitorWithoutProbe.visitLabel(label);
		methodVisitorProbed.visitLabel(label);
	}

	@Override
	public void visitLdcInsn(final Object value) {
		methodVisitorWithoutProbe.visitLdcInsn(value);
		methodVisitorProbed.visitLdcInsn(value);
	}

	@Override
	public void visitIincInsn(final int varIndex, final int increment) {
		methodVisitorWithoutProbe.visitIincInsn(varIndex, increment);
		methodVisitorProbed.visitIincInsn(varIndex, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		methodVisitorWithoutProbe.visitTableSwitchInsn(min, max, dflt, labels);
		methodVisitorProbed.visitTableSwitchInsn(min, max, dflt, labels);

	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		methodVisitorWithoutProbe.visitLookupSwitchInsn(dflt, keys, labels);
		methodVisitorProbed.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		methodVisitorWithoutProbe.visitMultiANewArrayInsn(descriptor, numDimensions);
		methodVisitorProbed.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

	// @Override
	// public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
	//     return methodVisitorWithoutProbe.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	//     return methodVisitorProbed.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	// }

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		methodVisitorWithoutProbe.visitTryCatchBlock(start, end, handler, type);
		methodVisitorProbed.visitTryCatchBlock(start, end, handler, type);

	}

	// @Override
	// public AnnotationVisitor visitTryCatchAnnotation(
	//     final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
	//     return methodVisitorWithoutProbe.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
	// }

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		methodVisitorWithoutProbe.visitLocalVariable(name, descriptor, signature, start, end, index);
		methodVisitorProbed.visitLocalVariable(name, descriptor, signature, start, end, index);
	}

	// @Override
	// public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
	//     return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
	// }

	@Override
	public void visitLineNumber(final int line, final Label start) {
		methodVisitorWithoutProbe.visitLineNumber(line, start);
		methodVisitorProbed.visitLineNumber(line, start);
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		methodVisitorWithoutProbe.visitMaxs(maxStack, maxLocals);
		methodVisitorProbed.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitEnd() {
		methodVisitorWithoutProbe.visitEnd();
		methodVisitorProbed.visitEnd();
	}
}
