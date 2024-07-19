package io.unlogged.core.bytecode;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.unlogged.util.DistinctClassLogNameMap;

public class InitStaticTransformer extends MethodVisitor {

	private String fullClassName;
	private final String mapName;
	private HashSet<String> methodList;

	public InitStaticTransformer(MethodVisitor mv, String fullClassName, HashSet<String> methodList) {
		super(Opcodes.ASM7, mv);
		this.fullClassName = fullClassName;
		this.mapName = DistinctClassLogNameMap.getClassMapStore(fullClassName);
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
