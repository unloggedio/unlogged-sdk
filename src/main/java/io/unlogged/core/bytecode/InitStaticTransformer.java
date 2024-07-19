package io.unlogged.core.bytecode;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InitStaticTransformer extends MethodVisitor {

	private HashSet<String> methodList;

	public InitStaticTransformer(MethodVisitor mv, HashSet<String> methodList) {
		super(Opcodes.ASM7, mv);
		this.methodList = methodList;
	}

	@Override
	public void visitCode() {
		mv.visitCode();

		for (String localMethod: this.methodList) {
			// This adds the line: Runtime.registerMethod(methodName)
			mv.visitLdcInsn(localMethod);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/Runtime", "registerMethod", "(Ljava/lang/String;)V", false);
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
