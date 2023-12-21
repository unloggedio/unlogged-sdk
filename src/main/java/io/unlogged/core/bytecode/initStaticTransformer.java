package io.unlogged.core.bytecode;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InitStaticTransformer extends MethodVisitor {

	private String className;
	private HashSet<String> methodList;

	public InitStaticTransformer(MethodVisitor mv, String className, HashSet<String> methodList) {
		super(Opcodes.ASM7, mv);
		this.className = className;
		this.methodList = methodList;
	}

	@Override
	public void visitCode() {
		mv.visitCode();
		mv.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(
			Opcodes.INVOKESPECIAL,
			"java/util/HashMap",
			"<init>",
			"()V",
			false
		);
		mv.visitFieldInsn(
			Opcodes.PUTSTATIC, 
			className,
			"map_store",
			"Ljava/util/Map;"
		);

		for (String localMethod: this.methodList) { 
			mv.visitLdcInsn(localMethod);
			mv.visitLdcInsn(0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "map_store", "put", "(Ljava/lang/String;I)V", false);
		}
	}
}