package io.unlogged.core.bytecode;
import org.objectweb.asm.*;

class MethodVisitorWithoutProbe extends MethodVisitor {

	private String methodName;
	private String className;

	public MethodVisitorWithoutProbe(int api, String methodName, String className, MethodVisitor mv) {
		super(api, mv);
		this.methodName = methodName;
		this.className = className;
	}


	@Override
	public void visitCode() {
		// Add the line: this.map_store.put(this.method_name, map_store.get(method_name) + 1);
		mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
		
		mv.visitFieldInsn(Opcodes.GETFIELD, this.className, "map_store", "Ljava/util/Map;");


		mv.visitLdcInsn(this.methodName);
		mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
		
		mv.visitFieldInsn(Opcodes.GETFIELD, this.className, "map_store", "Ljava/util/Map;"); // Load map_store onto the stack

		mv.visitLdcInsn(this.methodName);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		mv.visitInsn(Opcodes.ICONST_1); // Load the constant 1 onto the stack
		mv.visitInsn(Opcodes.IADD); // Add the values
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
		// Pop the result (discard it)
		mv.visitInsn(Opcodes.POP);

		super.visitCode();
	}
}
