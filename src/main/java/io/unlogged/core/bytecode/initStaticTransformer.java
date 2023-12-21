package io.unlogged.core.bytecode;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InitStaticTransformer extends MethodVisitor {

	private String fullClassName;
	private HashSet<String> methodList;

	public InitStaticTransformer(MethodVisitor mv, String fullClassName, HashSet<String> methodList) {
		super(Opcodes.ASM7, mv);
		this.fullClassName = fullClassName;
		this.methodList = methodList;
	}

	@Override
	public void visitCode() {
		mv.visitCode();

		// Instantiate HashMap<String, Integer>
		mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(java.util.HashMap.class));
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(
				Opcodes.INVOKESPECIAL,
				Type.getInternalName(java.util.HashMap.class),
				"<init>",
				"()V",
				false
		);

		// Store the instance in the static field map_store
		mv.visitFieldInsn(
				Opcodes.PUTSTATIC,
				fullClassName,
				"map_store",
				Type.getDescriptor(java.util.HashMap.class)
		);

		for (String localMethod: this.methodList) { 
			mv.visitLdcInsn(localMethod);
			mv.visitLdcInsn(0);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "map_store", "put", "(Ljava/lang/String;I)V", false);
		}

		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(2, 0);
		mv.visitEnd();
	}
}