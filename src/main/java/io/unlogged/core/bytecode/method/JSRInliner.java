package io.unlogged.core.bytecode.method;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * This class enables a wrapped visitor to analyze bytecode instructions after JSR inlining. 
 * @author ishio
 */
public class JSRInliner extends JSRInlinerAdapter {

	private MethodTransformer analysis;
	
	/**
	 * Create an instance of the object
	 * @param mv specifies a visitor to be executed on inlined bytecode instructions. 
	 * @param access is method modifiers 
	 * @param name is a method name 
	 * @param desc is a parameter descriptor
	 * @param signature is a generics signature
	 * @param exceptions specifies exceptions thrown by the method 
	 */
	public JSRInliner(MethodTransformer mv, int access, String name, String desc, String signature, String[] exceptions) {
		// The second parameter is null so that the object delays the execution of the given MethodTransformer
		super(Opcodes.ASM9, null, access, name, desc, signature, exceptions);
		this.analysis = mv;
	}
	
	/**
	 * A call back from MethodVisitor.
	 * Since this method is called to finish the JSR inlining process, 
	 * this object executes analysis after the inlining. 
	 */
	@Override
	public void visitEnd() {
		// Inline JSR instructions 
		super.visitEnd();
		
		// Provide the resultant instruction list for creating a list of labels in the method 
		analysis.setup(localVariables, instructions);
		
		// Analyze the inlined method
		super.accept(analysis);
	}	
	
}
