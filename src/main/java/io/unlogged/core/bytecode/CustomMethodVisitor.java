package io.unlogged.core.bytecode;

import org.objectweb.asm.*;

public class CustomMethodVisitor extends MethodVisitor {

	private final MethodVisitor mv_unprobed;
	private final MethodVisitor mv_probed;

	public CustomMethodVisitor(MethodVisitor mv_unprobed, MethodVisitor mv_probed) {
		super(Opcodes.ASM7);
		this.mv_unprobed = mv_unprobed;
		this.mv_probed = mv_probed;
	}

	// Override methods from MethodVisitor and delegate to both mv1 and mv2 as needed

	@Override
	public void visitInsn(int opcode) {
		mv_unprobed.visitInsn(opcode);
		mv_probed.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		mv_unprobed.visitIntInsn(opcode, operand);
		mv_probed.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		mv_unprobed.visitVarInsn(opcode, varIndex);
		mv_probed.visitVarInsn(opcode, varIndex);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
		if (mv_unprobed != null) {
			return mv_unprobed.visitAnnotation(descriptor, visible);
		}
		return null;
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		mv_unprobed.visitTypeInsn(opcode, type);
		mv_probed.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		mv_unprobed.visitFieldInsn(opcode, owner, name, descriptor);
		mv_probed.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		mv_unprobed.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
		mv_probed.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		mv_unprobed.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		mv_probed.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		mv_unprobed.visitJumpInsn(opcode, label);
		mv_probed.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(final Label label) {
		mv_unprobed.visitLabel(label);
		mv_probed.visitLabel(label);
	}

	@Override
	public void visitLdcInsn(final Object value) {
		mv_unprobed.visitLdcInsn(value);
		mv_probed.visitLdcInsn(value);
	}

	@Override
	public void visitIincInsn(final int varIndex, final int increment) {
		mv_unprobed.visitIincInsn(varIndex, increment);
		mv_probed.visitIincInsn(varIndex, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		mv_unprobed.visitTableSwitchInsn(min, max, dflt, labels);
		mv_probed.visitTableSwitchInsn(min, max, dflt, labels);

	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		mv_unprobed.visitLookupSwitchInsn(dflt, keys, labels);
		mv_probed.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		mv_unprobed.visitMultiANewArrayInsn(descriptor, numDimensions);
		mv_probed.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

	// @Override
	// public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
	//     return mv_unprobed.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	//     return mv_probed.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
	// }

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		mv_unprobed.visitTryCatchBlock(start, end, handler, type);
		mv_probed.visitTryCatchBlock(start, end, handler, type);

	}

	// @Override
	// public AnnotationVisitor visitTryCatchAnnotation(
	//     final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
	//     return mv_unprobed.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
	// }

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		mv_unprobed.visitLocalVariable(name, descriptor, signature, start, end, index);
		mv_probed.visitLocalVariable(name, descriptor, signature, start, end, index);
	}

	// @Override
	// public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
	//     return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
	// }

	@Override
	public void visitLineNumber(final int line, final Label start) {
		mv_unprobed.visitLineNumber(line, start);
		mv_probed.visitLineNumber(line, start);
	}

	@Override
	public void visitMaxs(final int maxStack, final int maxLocals) {
		mv_unprobed.visitMaxs(maxStack, maxLocals);
		mv_probed.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitEnd() {
		mv_unprobed.visitEnd();
		mv_probed.visitEnd();
	}
}
