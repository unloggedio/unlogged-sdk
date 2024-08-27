package io.unlogged.core.processor;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import io.unlogged.UnloggedLoggingLevel;
import io.unlogged.Unlogged;
import io.unlogged.UnloggedMode;
import io.unlogged.core.CleanupRegistry;
import io.unlogged.core.DiagnosticsReceiver;
import io.unlogged.core.javac.Javac;
import io.unlogged.core.javac.JavacTransformer;
import sun.misc.Unsafe;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @version 1.0
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UnloggedProcessor extends AbstractProcessor {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final IdentityHashMap<JCTree.JCCompilationUnit, Long> roots = new IdentityHashMap<JCTree.JCCompilationUnit, Long>();
    private final CleanupRegistry cleanup = new CleanupRegistry();
    public Element transformedEntryPoint = null;
    private ProcessingEnvironment unwrappedProcessingEnv;
    //    private Context context;
    private JavacProcessingEnvironment javacProcessingEnv;
    private Trees trees;
    private JavacTransformer transformer;
    private JavacFiler javacFiler;
	private UnloggedProcessorConfig unloggedProcessorConfig = new UnloggedProcessorConfig(1, UnloggedLoggingLevel.COUNTER, UnloggedMode.LogAll);
	private boolean parsed = false;


    public UnloggedProcessor() {
//        System.out.println("HelloUnloggedProcessor");
    }

    private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader()
                    .loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        } catch (Throwable ignored) {
        }
        return unwrapped != null ? unwrapped : wrapper;
    }

    /**
     * Useful from jdk9 and up; required from jdk16 and up. This code is supposed to gracefully do nothing on jdk8 and below, as this operation isn't needed there.
     */
    public static void addOpensForLombok() {
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            return; //jdk8-; this is not needed.
        }

        Unsafe unsafe = getUnsafe();
        Object jdkCompilerModule = getJdkCompilerModule();
        Object ownModule = getOwnModule();
        String[] allPkgs = {
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.comp",
                "com.sun.tools.javac.file",
                "com.sun.tools.javac.main",
                "com.sun.tools.javac.model",
                "com.sun.tools.javac.parser",
                "com.sun.tools.javac.processing",
                "com.sun.tools.javac.tree",
                "com.sun.tools.javac.util",
                "com.sun.tools.javac.jvm",
        };

        try {
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            long firstFieldOffset = getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(m, firstFieldOffset, true);
            for (String p : allPkgs) m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {
        }
    }

    private static long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (NoSuchFieldException e) {
            // can't happen.
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    private static Object getOwnModule() {
        try {
            Method m = Permit.getMethod(Class.class, "getModule");
            return m.invoke(UnloggedProcessor.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getJdkCompilerModule() {
		/* call public api: ModuleLayer.boot().findModule("jdk.compiler").get();
		   but use reflection because we don't want this code to crash on jdk1.7 and below.
		   In that case, none of this stuff was needed in the first place, so we just exit via
		   the catch block and do nothing.
		 */

        try {
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);
            Class<?> cOptional = Class.forName("java.util.Optional");
            Method mFindModule = cModuleLayer.getDeclaredMethod("findModule", String.class);
            Object oCompilerO = mFindModule.invoke(bootLayer, "jdk.compiler");
            return cOptional.getDeclaredMethod("get").invoke(oCompilerO);
        } catch (Exception e) {
            return null;
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static ClassLoader wrapClassLoader(final ClassLoader parent) {
        return new ClassLoader() {

            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return parent.loadClass(name);
            }

            public String toString() {
                return parent.toString();
            }

            public URL getResource(String name) {
                return parent.getResource(name);
            }

            public Enumeration<URL> getResources(String name) throws IOException {
                return parent.getResources(name);
            }

            public InputStream getResourceAsStream(String name) {
                return parent.getResourceAsStream(name);
            }

            public void setDefaultAssertionStatus(boolean enabled) {
                parent.setDefaultAssertionStatus(enabled);
            }

            public void setPackageAssertionStatus(String packageName, boolean enabled) {
                parent.setPackageAssertionStatus(packageName, enabled);
            }

            public void setClassAssertionStatus(String className, boolean enabled) {
                parent.setClassAssertionStatus(className, enabled);
            }

            public void clearAssertionStatus() {
                parent.clearAssertionStatus();
            }
        };
    }

    private void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public synchronized void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);


        String agentArgs = "";
//        System.out.println("[unlogged] Starting agent: with arguments [" + agentArgs + "]");
        synchronized (initialized) {
            if (initialized.get()) {
                return;
            }
            initialized.set(true);
        }

//        this.processingEnv = procEnv;
        this.javacProcessingEnv = getJavacProcessingEnvironment(procEnv);
        this.javacFiler = getJavacFiler(procEnv.getFiler());

        placePostCompileAndDontMakeForceRoundDummiesHook();
        trees = Trees.instance(javacProcessingEnv);
        unwrappedProcessingEnv = jbUnwrap(ProcessingEnvironment.class, procEnv);
//        context = ((JavacProcessingEnvironment) unwrappedProcessingEnv).getContext();
        transformer = new JavacTransformer(procEnv.getMessager(), trees, javacProcessingEnv.getContext());


//        WeaveParameters weaveParameters = new WeaveParameters(agentArgs);
//        WeaveConfig weaveConfig = new WeaveConfig(weaveParameters);
//        weaver = new Weaver(new File("./session"), weaveConfig, treeMaker, elementUtils);

    }

    private void placePostCompileAndDontMakeForceRoundDummiesHook() {
        stopJavacProcessingEnvironmentFromClosingOurClassloader();

        forceMultipleRoundsInNetBeansEditor();
        Context context = javacProcessingEnv.getContext();
        disablePartialReparseInNetBeansEditor(context);
        try {
            Method keyMethod = Permit.getMethod(Context.class, "key", Class.class);
            Object key = Permit.invoke(keyMethod, context, JavaFileManager.class);
            Field htField = Permit.getField(Context.class, "ht");
            @SuppressWarnings("unchecked")
            Map<Object, Object> ht = (Map<Object, Object>) Permit.get(htField, context);
            final JavaFileManager originalFiler = (JavaFileManager) ht.get(key);
//            System.err.println("JavaFileManager: " + originalFiler.getClass().getCanonicalName());
            if (!(originalFiler instanceof InterceptingJavaFileManager)) {
                final Messager messager = processingEnv.getMessager();
                DiagnosticsReceiver receiver = new MessagerDiagnosticsReceiver(messager);
                JavaFileManager newFilerManager = new InterceptingJavaFileManager(originalFiler, receiver, unloggedProcessorConfig);
                ht.put(key, newFilerManager);
                Field filerFileManagerField = Permit.getField(JavacFiler.class, "fileManager");
                filerFileManagerField.set(javacFiler, newFilerManager);

                if (Javac.getJavaCompilerVersion() > 8
                        && !io.unlogged.core.handlers.JavacHandlerUtil.inNetbeansCompileOnSave(context)) {
                    replaceFileManagerJdk9(context, newFilerManager);
                }
            }
        } catch (Exception e) {
            throw Util.sneakyThrow(e);
        }
    }

    private void replaceFileManagerJdk9(Context context, JavaFileManager newFiler) {
        try {
            JavaCompiler compiler = (JavaCompiler) Permit.invoke(
                    Permit.getMethod(JavaCompiler.class, "instance", Context.class), null, context);
            try {
                Field fileManagerField = Permit.getField(JavaCompiler.class, "fileManager");
                Permit.set(fileManagerField, compiler, newFiler);
            } catch (Exception e) {
            }

            try {
                Field writerField = Permit.getField(JavaCompiler.class, "writer");
                ClassWriter writer = (ClassWriter) writerField.get(compiler);
                Field fileManagerField = Permit.getField(ClassWriter.class, "fileManager");
                Permit.set(fileManagerField, writer, newFiler);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }

    private void disablePartialReparseInNetBeansEditor(Context context) {
        try {
            Class<?> cancelServiceClass = Class.forName("com.sun.tools.javac.util.CancelService");
            Method cancelServiceInstance = Permit.getMethod(cancelServiceClass, "instance", Context.class);
            Object cancelService = Permit.invoke(cancelServiceInstance, null, context);
            if (cancelService == null) return;
            Field parserField = Permit.getField(cancelService.getClass(), "parser");
            Object parser = parserField.get(cancelService);
            Field supportsReparseField = Permit.getField(parser.getClass(), "supportsReparse");
            supportsReparseField.set(parser, false);
        } catch (ClassNotFoundException e) {
            // only NetBeans has it
        } catch (NoSuchFieldException e) {
            // only NetBeans has it
        } catch (Throwable t) {
            throw Util.sneakyThrow(t);
        }
    }

    private void forceMultipleRoundsInNetBeansEditor() {
        try {
            Field f = Permit.getField(JavacProcessingEnvironment.class, "isBackgroundCompilation");
            f.set(javacProcessingEnv, true);
        } catch (NoSuchFieldException e) {
            // only NetBeans has it
        } catch (Throwable t) {
            throw Util.sneakyThrow(t);
        }
    }

    private void stopJavacProcessingEnvironmentFromClosingOurClassloader() {
        try {
            Field f = Permit.getField(JavacProcessingEnvironment.class, "processorClassLoader");
            ClassLoader unwrapped = (ClassLoader) f.get(javacProcessingEnv);
            if (unwrapped == null) return;
            ClassLoader wrapped = wrapClassLoader(unwrapped);
            f.set(javacProcessingEnv, wrapped);
        } catch (NoSuchFieldException e) {
            // Some versions of javac have this (and call close on it), some don't. I guess this one doesn't have it.
        } catch (Throwable t) {
            throw Util.sneakyThrow(t);
        }
    }

    /**
     * This class returns the given filer as a JavacFiler. In case the filer is no
     * JavacFiler (e.g. the Gradle IncrementalFiler), its "delegate" field is used to get the JavacFiler
     * (directly or through a delegate field again)
     */
    public JavacFiler getJavacFiler(Object filer) {
        if (filer instanceof JavacFiler) return (JavacFiler) filer;

        // try to find a "delegate" field in the object, and use this to check for a JavacFiler
        for (Class<?> filerClass = filer.getClass(); filerClass != null; filerClass = filerClass.getSuperclass()) {
            Object delegate = tryGetDelegateField(filerClass, filer);
            if (delegate == null) delegate = tryGetProxyDelegateToField(filerClass, filer);
            if (delegate == null) delegate = tryGetFilerField(filerClass, filer);

            if (delegate != null) return getJavacFiler(delegate);
            // delegate field was not found, try on superclass
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Can't get a JavacFiler from " + filer.getClass().getName() + ". Lombok won't work.");
        return null;
    }

    /**
     * Kotlin incremental processing
     */
    private Object tryGetFilerField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "filer").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (System.getProperty("unlogged.disable", null) != null) return false;


		Set<? extends Element> annotatedClasses = roundEnv.getElementsAnnotatedWith(Unlogged.class);
		for (Element element : annotatedClasses) {
			this.parsed = true;
			Unlogged unlogged = element.getAnnotation(Unlogged.class);
			
			// setup unloggedProcessorConfig
			long defaultCounter = Long.parseLong(unlogged.counter());
            this.unloggedProcessorConfig.setDefaultCounter(defaultCounter);
			this.unloggedProcessorConfig.setUnloggedLoggingLevel(unlogged.unloggedLoggingLevel());
            this.unloggedProcessorConfig.setUnloggedMode(unlogged.unloggedMode());
		}

        if (roundEnv.processingOver()) {
            transformer.finish(javacProcessingEnv.getContext(), cleanup);
            return false;
        }

		if (annotatedClasses.isEmpty() && !this.parsed) {
			// no class is annotated with Unlogged
			// set log mode as LogNothing
			this.parsed = true;
			this.unloggedProcessorConfig.setUnloggedMode(UnloggedMode.LogNothing);
		}
		else if (annotatedClasses.size() > 1) {
            throw new RuntimeException("More than 1 class annotated with @Unlogged annotation. Only the entry point " +
                    "method should be annotated with @Unlogged annotation: [" +
                    annotatedClasses.stream()
                            .map(Element::getSimpleName)
                            .collect(Collectors.toList()) + "]");
        }
//        else if (annotatedClasses.size() == 1) {
//            Element entryPoint = annotatedClasses.stream().findFirst().get();
//            if (transformedEntryPoint == null) {
//                // first entry point
//                transformedEntryPoint = entryPoint;
////                transformer.transformEntryPoint(context, transformedEntryPoint, cleanup);
//            } else if (transformedEntryPoint != entryPoint) {
//                throw new RuntimeException(
//                        "More than 1 class annotated with @Unlogged annotation. Only the entry point " +
//                                "method should be annotated with @Unlogged annotation");
//            }
//        }
//
        Set<? extends Element> rootElements = roundEnv.getRootElements();

        for (Element element : rootElements) {
            JCTree.JCCompilationUnit unit = toUnit(element);
            if (unit == null) continue;
            if (roots.containsKey(unit)) continue;
            roots.put(unit, 0L);
        }

        transformer.transform(javacProcessingEnv.getContext(), new ArrayList<>(roots.keySet()), cleanup);

        return true;
    }

    private JCTree.JCCompilationUnit toUnit(Element element) {
        TreePath path = null;
        if (trees != null) {
            try {
                path = trees.getPath(element);
            } catch (NullPointerException ignore) {
                // Happens if a package-info.java doesn't contain a package declaration.
                // https://github.com/projectlombok/lombok/issues/2184
                // We can safely ignore those, since they do not need any processing
            }
        }
        if (path == null) return null;

        return (JCTree.JCCompilationUnit) path.getCompilationUnit();
    }


    /**
     * This class casts the given processing environment to a JavacProcessingEnvironment. In case of
     * gradle incremental compilation, the delegate ProcessingEnvironment of the gradle wrapper is returned.
     */
    public JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv) {
        addOpensForLombok();
        if (procEnv instanceof JavacProcessingEnvironment) return (JavacProcessingEnvironment) procEnv;

        // try to find a "delegate" field in the object, and use this to try to obtain a JavacProcessingEnvironment
        for (Class<?> procEnvClass = procEnv.getClass(); procEnvClass != null; procEnvClass = procEnvClass.getSuperclass()) {
            Object delegate = tryGetDelegateField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProxyDelegateToField(procEnvClass, procEnv);
            if (delegate == null) delegate = tryGetProcessingEnvField(procEnvClass, procEnv);

            if (delegate != null) return getJavacProcessingEnvironment(delegate);
            // delegate field was not found, try on superclass
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "Can't get the delegate of the gradle IncrementalProcessingEnvironment. Lombok won't work.");
        return null;
    }

    /**
     * Gradle incremental processing
     */
    private Object tryGetDelegateField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "delegate").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IntelliJ IDEA >= 2020.3
     */
    private Object tryGetProxyDelegateToField(Class<?> delegateClass, Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kotlin incremental processing
     */
    private Object tryGetProcessingEnvField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "processingEnv").get(instance);
        } catch (Exception e) {
            return null;
        }
    }


}
