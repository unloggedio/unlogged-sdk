package io.unlogged.core.bytecode;

import io.unlogged.core.bytecode.method.JSRInliner;
import io.unlogged.core.bytecode.method.MethodTransformer;
import io.unlogged.logging.util.TypeIdUtil;
import io.unlogged.weaver.TypeHierarchy;
import io.unlogged.weaver.WeaveLog;
import net.bytebuddy.implementation.bind.annotation.This;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import java.io.IOException;

/**
 * This class weaves logging code into a Java class file.
 * The constructors execute the weaving process.
 */
public class ClassTransformer extends ClassVisitor {

    private final WeaveConfig config;
    private final ClassWriter classWriter;
    private final String PACKAGE_SEPARATOR = "/";
    private String[] interfaces;
    private String superName;
    private String signature;
    private WeaveLog weavingInfo;
    private String fullClassName;
    private String className;
    private String outerClassName;
    private String packageName;
    private String sourceFileName;
    private byte[] weaveResult;
    private String classLoaderIdentifier;

    /**
     * This constructor weaves the given class and provides the result.
     *
     * @param weaver     specifies the state of the weaver.
     * @param config     specifies the configuration.
     * @param inputClass specifies a byte array containing the target class.
     * @throws IOException may be thrown if an error occurs during the weaving.
     */
    public ClassTransformer(WeaveLog weaver, WeaveConfig config, byte[] inputClass, TypeHierarchy typeHierarchy) throws IOException {
        this(weaver, config, new ClassReader(inputClass), typeHierarchy);
    }

    /**
     * This constructor weaves the given class and provides the result.
     *
     * @param weaver specifies the state of the weaver.
     * @param config specifies the configuration.
     * @param reader specifies a class reader to read the target class.
     */
    public ClassTransformer(WeaveLog weaver, WeaveConfig config, ClassReader reader, TypeHierarchy typeHierarchy) {
        // Create a writer for the target class
        this(weaver, config, new FixedClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, typeHierarchy));
        // Start weaving, and store the result to a byte array
        reader.accept(this, ClassReader.EXPAND_FRAMES);
        weaveResult = classWriter.toByteArray();
        classLoaderIdentifier = TypeIdUtil.getClassLoaderIdentifier(weaver.getFullClassName());
    }

    /**
     * Initializes the object as a ClassVisitor.
     *c
     * @param weaver specifies the state of the weaver.
     * @param config specifies the configuration.
     * @param cw     specifies the class writer (MetracerClassWriter).
     */
    protected ClassTransformer(WeaveLog weaver, WeaveConfig config, ClassWriter cw) {
        super(Opcodes.ASM9, cw);
        this.weavingInfo = weaver;
        this.config = config;
        this.classWriter = cw;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
//		System.err.println("Visit annotation: " + descriptor + " on class: " + className);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
//		System.err.println("Visit type annotation: " + typePath);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    /**
     * @return the weaving result.
     */
    public byte[] getWeaveResult() {
        return weaveResult;
    }

    /**
     * @return the full class name including the package name and class name
     */
    public String getFullClassName() {
        return fullClassName;
    }

    /**
     * @return the class name without the package name
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return the class loader identifier
     */
    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }

    /**
     * A call back from the ClassVisitor.
     * Record the class information to fields.
     */
    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
//		System.err.println("Visit class ["+ name + "]");
        this.fullClassName = name;
        this.weavingInfo.setFullClassName(fullClassName);
        int index = name.lastIndexOf(PACKAGE_SEPARATOR);
        this.interfaces = interfaces;
        this.superName = superName;
        this.signature = signature;
        if (index >= 0) {
            packageName = name.substring(0, index);
            className = name.substring(index + 1);
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * A call back from the ClassVisitor.
     * Record the source file name.
     */
    @Override
    public void visitSource(String source, String debug) {
//		System.err.println("Visit source ["+ source + "] + [" + debug + "]");
        super.visitSource(source, debug);
        sourceFileName = source;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    /**
     * A call back from the ClassVisitor.
     * Record the outer class name if this class is an inner class.
     */
    @Override
    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
//		System.err.println("Visit innerClass ["+ name + "] + [" + outerName + "] + [" + innerName + "]");
        super.visitInnerClass(name, outerName, innerName, access);
        if (name.equals(fullClassName)) {
            outerClassName = outerName;
        }
    }

    /**
     * A call back from the ClassVisitor.
     * Create an instance of a MethodVisitor that inserts logging code into a method.
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {


		// create probed method

        MethodVisitor method_visitor_probe;
		String name_probed = name + "_PROBED";
		MethodVisitor mv_probed = super.visitMethod(access, name_probed , desc, signature, exceptions);
        if (mv_probed != null) {

            mv_probed = new TryCatchBlockSorter(mv_probed, access, name, desc, signature, exceptions);
            MethodTransformer transformer_probed = new MethodTransformer(
                    weavingInfo, config, sourceFileName,
                    fullClassName, outerClassName, access,
                    name, desc, signature, exceptions, mv_probed
            );

            method_visitor_probe = new JSRInliner(transformer_probed, access, name, desc, signature, exceptions);
        }
        else {
            method_visitor_probe = null;
        }

		// String name_simple = name + "_SIMPLE";
		MethodVisitor mv_unprobed = super.visitMethod(access, name, desc, signature, exceptions);

		// early exit with probes
		Label exitLabel = new Label();

		// Assuming your condition is some integer comparison (e.g., if i > 0)
		mv_unprobed.visitVarInsn(Opcodes.ILOAD, 1); // Load the variable onto the stack
		mv_unprobed.visitJumpInsn(Opcodes.IFLE, exitLabel); // Jump to exitLabel if less than or equal to 0
	
		// add data to stack
		Type[] argumentTypes = Type.getArgumentTypes(desc);
		for (int i = 0; i <= argumentTypes.length-1; i++) {
			pushArgument(mv_unprobed, i + 1, argumentTypes[i]);
		}

		mv_unprobed.visitMethodInsn(Opcodes.INVOKESTATIC, "this", name_probed, desc, false);
		// Return the result from the method
		mv_unprobed.visitInsn(Opcodes.IRETURN);

		// Exit label
		mv_unprobed.visitLabel(exitLabel);
		return new CustomMethodVisitor(mv_unprobed, method_visitor_probe);
    }

	private void pushArgument(MethodVisitor mv, int argIndex, Type argType) {
        // Determine the opcode based on the argument type
        int opcode;
        if (argType.equals(Type.INT_TYPE) || argType.equals(Type.BOOLEAN_TYPE) || argType.equals(Type.CHAR_TYPE) || argType.equals(Type.SHORT_TYPE) || argType.equals(Type.BYTE_TYPE)) {
            opcode = Opcodes.ILOAD;
        } else if (argType.equals(Type.FLOAT_TYPE)) {
            opcode = Opcodes.FLOAD;
        } else if (argType.equals(Type.LONG_TYPE)) {
            opcode = Opcodes.LLOAD;
        } else if (argType.equals(Type.DOUBLE_TYPE)) {
            opcode = Opcodes.DLOAD;
        } else {
            opcode = Opcodes.ALOAD;
        }

        // Load the argument onto the stack
        mv.visitVarInsn(opcode, argIndex);

        // If the argument type is double or long, increment the index again
        if (argType.equals(Type.LONG_TYPE) || argType.equals(Type.DOUBLE_TYPE)) {
            argIndex++;
        }
    }

    private static class CustomMethodVisitor extends MethodVisitor {

        private final MethodVisitor method_visitor_1;
        private final MethodVisitor method_visitor_2;

        public CustomMethodVisitor(MethodVisitor method_visitor_1, MethodVisitor method_visitor_2) {
            super(Opcodes.ASM7);
            this.method_visitor_1 = method_visitor_1;
            this.method_visitor_2 = method_visitor_2;
        }

        // Override methods from MethodVisitor and delegate to both mv1 and mv2 as needed

        @Override
        public void visitInsn(int opcode) {
            method_visitor_1.visitInsn(opcode);
            method_visitor_2.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            method_visitor_1.visitIntInsn(opcode, operand);
            method_visitor_2.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            method_visitor_1.visitVarInsn(opcode, varIndex);
            method_visitor_2.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            method_visitor_1.visitTypeInsn(opcode, type);
            method_visitor_2.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            method_visitor_1.visitFieldInsn(opcode, owner, name, descriptor);
            method_visitor_2.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            method_visitor_1.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
            method_visitor_2.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            method_visitor_1.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            method_visitor_2.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            method_visitor_1.visitJumpInsn(opcode, label);
            method_visitor_2.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(final Label label) {
            method_visitor_1.visitLabel(label);
            method_visitor_2.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(final Object value) {
            method_visitor_1.visitLdcInsn(value);
            method_visitor_2.visitLdcInsn(value);
        }

        @Override
        public void visitIincInsn(final int varIndex, final int increment) {
            method_visitor_1.visitIincInsn(varIndex, increment);
            method_visitor_2.visitIincInsn(varIndex, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            method_visitor_1.visitTableSwitchInsn(min, max, dflt, labels);
            method_visitor_2.visitTableSwitchInsn(min, max, dflt, labels);

        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            method_visitor_1.visitLookupSwitchInsn(dflt, keys, labels);
            method_visitor_2.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            method_visitor_1.visitMultiANewArrayInsn(descriptor, numDimensions);
            method_visitor_2.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        // @Override
        // public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        //     return method_visitor_1.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
        //     return method_visitor_2.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
        // }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            method_visitor_1.visitTryCatchBlock(start, end, handler, type);
            method_visitor_2.visitTryCatchBlock(start, end, handler, type);

        }

        // @Override
        // public AnnotationVisitor visitTryCatchAnnotation(
        //     final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        //     return method_visitor_1.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
        // }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            method_visitor_1.visitLocalVariable(name, descriptor, signature, start, end, index);
            method_visitor_2.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        // @Override
        // public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        //     return mv.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
        // }

        @Override
        public void visitLineNumber(final int line, final Label start) {
            method_visitor_1.visitLineNumber(line, start);
            method_visitor_2.visitLineNumber(line, start);
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            method_visitor_1.visitMaxs(maxStack, maxLocals);
            method_visitor_2.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitEnd() {
            method_visitor_1.visitEnd();
            method_visitor_2.visitEnd();
        }
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public String getSuperName() {
        return superName;
    }

    public String getSignature() {
        return signature;
    }
}
