package io.unlogged.core.bytecode;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.unlogged.Constants;

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

		// Store the instance in the static field mapStore
		mv.visitFieldInsn(
				Opcodes.PUTSTATIC,
				fullClassName,
				Constants.mapStoreCompileValue,
				Type.getDescriptor(java.util.HashMap.class)
		);

		for (String localMethod: this.methodList) { 
			mv.visitFieldInsn(
				Opcodes.GETSTATIC,
				this.fullClassName,
				Constants.mapStoreCompileValue,
				"Ljava/util/HashMap;"
			);

			mv.visitLdcInsn(localMethod);
			mv.visitLdcInsn(0L);

			// cast long_object to long_primitive
			mv.visitMethodInsn(
				Opcodes.INVOKESTATIC,
				Type.getInternalName(Long.class),
				"valueOf",
				"(J)Ljava/lang/Long;",
				false
			);

			mv.visitMethodInsn(
				Opcodes.INVOKEVIRTUAL,
				Type.getInternalName(java.util.HashMap.class),
				"put",
				"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
				false
			);

			mv.visitInsn(Opcodes.POP);
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
