package io.unlogged.core.bytecode;

import io.unlogged.Constants;
import io.unlogged.core.bytecode.method.JSRInliner;
import io.unlogged.core.bytecode.method.MethodTransformer;
import io.unlogged.logging.util.TypeIdUtil;
import io.unlogged.util.ProbeFlagUtil;
import io.unlogged.weaver.TypeHierarchy;
import io.unlogged.weaver.WeaveLog;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.TryCatchBlockSorter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
	private HashMap<String, Long> classCounterMap = new HashMap<>();
	private boolean hasStaticInitialiser;
	private boolean alwaysProbeClassFlag = false;

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

	private MethodVisitor addProbe (MethodVisitor methodVisitorProbed, int access, String name, String desc, String[] exceptions) {
		if (methodVisitorProbed != null) {
			methodVisitorProbed = new TryCatchBlockSorter(methodVisitorProbed, access, name, desc, signature, exceptions);
			MethodTransformer transformerProbed = new MethodTransformer(
					weavingInfo, config, sourceFileName,
					fullClassName, outerClassName, access,
					name, desc, signature, exceptions, methodVisitorProbed
			);

			methodVisitorProbed = new JSRInliner(transformerProbed, access, name, desc, signature, exceptions);
		}
		return methodVisitorProbed;
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
						long valueLong = Long.parseLong((String)value);
						classCounterMap.put(className, valueLong);
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

		this.alwaysProbeClassFlag = ProbeFlagUtil.getAlwaysProbeClassFlag(access);
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
		
		// calcuclate probe flag at method level 
		Boolean alwaysProbeMethodFlag = this.alwaysProbeClassFlag || ProbeFlagUtil.getalwaysProbeMethodFlag(name, access, desc);
		Boolean neverProbeMethodFlag = ProbeFlagUtil.getNeverProbeMethodFlag(name);

		// early exit for clinit. It is already defined in class with initial method
		if (name.equals("<clinit>")) {
			this.hasStaticInitialiser = true;
			MethodVisitor methodVisitorProbed = super.visitMethod(access, name, desc, signature, exceptions);

			FieldVisitor fieldVisitor = visitField(Opcodes.ACC_STATIC, Constants.mapStoreCompileValue, "Ljava/util/HashMap;", null, null);
			fieldVisitor.visitEnd();

			methodVisitorProbed = new InitStaticTransformer(methodVisitorProbed, fullClassName, this.methodList);
			methodVisitorProbed = addProbe(methodVisitorProbed, access, name, desc, exceptions);
			return methodVisitorProbed;
		}

		// early exit got method that are never probed
		if (neverProbeMethodFlag) {
			MethodVisitor methodVisitorUnModified = cv.visitMethod(access, name, desc, signature, exceptions);
            return methodVisitorUnModified;
        }
		
		// early exit for method that are always probed
		if (alwaysProbeMethodFlag) {
			MethodVisitor methodVisitorProbed = super.visitMethod(access, name, desc, signature, exceptions);
			methodVisitorProbed = addProbe(methodVisitorProbed, access, name, desc, exceptions);
			return methodVisitorProbed;
		}

		this.methodList.add(name);
		String nameProbed = name + Constants.probedValue;
		MethodVisitor methodVisitorProbed = super.visitMethod(access, nameProbed , desc, signature, exceptions);
		methodVisitorProbed = addProbe(methodVisitorProbed, access, nameProbed, desc, exceptions);
		
		long classCounter = getCounter(this.classCounterMap, className);
		MethodVisitorWithoutProbe methodVisitorWithoutProbe = new MethodVisitorWithoutProbe(api, name, fullClassName, access, desc, classCounter, super.visitMethod(access, name , desc, signature, exceptions));

		return new DualMethodVisitor(methodVisitorWithoutProbe, methodVisitorProbed);
    }

	@Override
    public void visitEnd() {
		
		if ((!this.hasStaticInitialiser) && (!this.alwaysProbeClassFlag)) {	
			// staticInitialiser is not defined and needed, define one

			FieldVisitor fieldVisitor = visitField(Opcodes.ACC_STATIC, Constants.mapStoreCompileValue, "Ljava/util/HashMap;", null, null);
			fieldVisitor.visitEnd();

			this.hasStaticInitialiser = true;
			MethodVisitor staticNew = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			staticNew.visitCode();

			// Instantiate HashMap<String, Integer>
			staticNew.visitTypeInsn(Opcodes.NEW, Type.getInternalName(java.util.HashMap.class));
			staticNew.visitInsn(Opcodes.DUP);
			staticNew.visitMethodInsn(
				Opcodes.INVOKESPECIAL,
				Type.getInternalName(java.util.HashMap.class),
				"<init>",
				"()V",
				false
			);

			// Store the instance in the static field mapStore
			staticNew.visitFieldInsn(
				Opcodes.PUTSTATIC,
				fullClassName,
				Constants.mapStoreCompileValue,
				Type.getDescriptor(java.util.HashMap.class)
			);

			for (String localMethod: this.methodList) { 
				staticNew.visitFieldInsn(
					Opcodes.GETSTATIC,
					this.fullClassName,
					Constants.mapStoreCompileValue,
					"Ljava/util/HashMap;"
				);
	
				staticNew.visitLdcInsn(localMethod);
				staticNew.visitLdcInsn(0L);

				// cast long object to long primitive
				staticNew.visitMethodInsn(
					Opcodes.INVOKESTATIC,
					Type.getInternalName(Long.class),
					"valueOf",
					"(J)Ljava/lang/Long;",
					false
				);
	
				staticNew.visitMethodInsn(
					Opcodes.INVOKEVIRTUAL,
					Type.getInternalName(java.util.HashMap.class),
					"put",
					"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
					false
				);
	
				staticNew.visitInsn(Opcodes.POP);
			}
	
			staticNew.visitInsn(Opcodes.RETURN);
			staticNew.visitMaxs(0, 0);
			staticNew.visitEnd();
		}
		
		super.visitEnd();
    }


	private long getCounter (Map<String, Long> mapCounter, String key) {
		if (mapCounter.get(key) == null) {
			return (long)0;
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
