package io.unlogged.core.bytecode;

import io.unlogged.core.bytecode.method.JSRInliner;
import io.unlogged.core.bytecode.method.MethodTransformer;
import io.unlogged.logging.util.TypeIdUtil;
import io.unlogged.weaver.TypeHierarchy;
import io.unlogged.weaver.WeaveLog;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

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
	private HashSet<String> methodList = new HashSet<>();
	private HashMap<String, Integer> classCounterMap = new HashMap<>();
	private boolean hasStaticInitialiser;

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
		// check for the annotation @UnloggedClass
		if ("Lio/unlogged/UnloggedClass;".equals(descriptor)) {
			return new AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
				@Override
				public void visit(String key, Object value) {
					// check for key string
					if ("loggingFrequency".equals(key)) {
						Integer valueInteger = Integer.parseInt((String)value);
						classCounterMap.put(className, valueInteger);
					}
					super.visit(key, value);
				}
			};
		}
		return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
//		System.err.println("Visit type annotation: " + typePath);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

	@Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return super.visitField(access, name, descriptor, signature, value);
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
		
		MethodVisitor mv_probed;
		if (name.equals("<clinit>")) {
			// clinit is made before this step
			this.hasStaticInitialiser = true;
			mv_probed = super.visitMethod(access, name, desc, signature, exceptions);
			mv_probed = new initStaticTransformer(mv_probed, className, this.methodList);
		}
		else if (name.equals("<init>")){
			// constructor method
			mv_probed = super.visitMethod(access, name , desc, signature, exceptions);
		}
		else {
			this.methodList.add(name);
			String name_probed = name + "_PROBED";
			mv_probed = super.visitMethod(access, name_probed , desc, signature, exceptions);
		}

		// add probe
		if (mv_probed != null) {
			mv_probed = new TryCatchBlockSorter(mv_probed, access, name, desc, signature, exceptions);
			MethodTransformer transformer_probed = new MethodTransformer(
					weavingInfo, config, sourceFileName,
					fullClassName, outerClassName, access,
					name, desc, signature, exceptions, mv_probed
			);

			mv_probed = new JSRInliner(transformer_probed, access, name, desc, signature, exceptions);
		}
		
		if (name.equals("<init>") || name.equals("<clinit>")) {
			return mv_probed;
		}

		int classCounter = getCounter(this.classCounterMap, className);
		MethodVisitorWithoutProbe mv_unprobed = new MethodVisitorWithoutProbe(api, name, className, desc, classCounter, super.visitMethod(access, name , desc, signature, exceptions));

		return new CustomMethodVisitor(mv_unprobed, mv_probed);
    }

	@Override
    public void visitEnd() {
		
		if (!this.hasStaticInitialiser) {	
			// staticInitialiser is not defined, define one
			this.hasStaticInitialiser = true;

			MethodVisitor staticNew = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			staticNew.visitCode();
			staticNew.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
			staticNew.visitInsn(Opcodes.DUP);
			staticNew.visitMethodInsn(
				Opcodes.INVOKESPECIAL,
				"java/util/HashMap",
				"<init>",
				"()V",
				false
			);
			staticNew.visitFieldInsn(
				Opcodes.PUTSTATIC,
				className,
				"map_store",
				"Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Integer;>;"
			);

			for (String localMethod: this.methodList) { 
				staticNew.visitLdcInsn(localMethod);
				staticNew.visitLdcInsn(0);
				staticNew.visitMethodInsn(Opcodes.INVOKESTATIC, "map_store", "put", "(Ljava/lang/String;I)V", false);
			}
			
			staticNew.visitInsn(Opcodes.RETURN);
			staticNew.visitMaxs(2, 0);
			staticNew.visitEnd();
		}
		
		super.visitEnd();
    }


	private int getCounter (HashMap<String, Integer> mapCounter, String key) {
		if (mapCounter.get(key) == null) {
			return 0;
		}
		else {
			return mapCounter.get(key);
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
