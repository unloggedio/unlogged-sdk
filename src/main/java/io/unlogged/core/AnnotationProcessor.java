package io.unlogged.core;

import io.unlogged.core.processor.Permit;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.unlogged.core.Augments.ClassLoader_lombokAlreadyAddedTo;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(value = SourceVersion.RELEASE_19)
public class AnnotationProcessor extends AbstractProcessor {

    private final List<ProcessorDescriptor> registered = Arrays.asList(new JavacDescriptor());
    private final List<String> delayedWarnings = new ArrayList<String>();
    private final List<ProcessorDescriptor> active = new ArrayList<ProcessorDescriptor>();

    private static String trace(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w, true));
        return w.toString();
    }

    /**
     * Gradle incremental processing
     */
    private static Object tryGetDelegateField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "delegate").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kotlin incremental processing
     */
    private static Object tryGetProcessingEnvField(Class<?> delegateClass, Object instance) {
        try {
            return Permit.getField(delegateClass, "processingEnv").get(instance);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * IntelliJ IDEA >= 2020.3
     */
    private static Object tryGetProxyDelegateToField(Class<?> delegateClass, Object instance) {
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(instance);
            return Permit.getField(handler.getClass(), "val$delegateTo").get(handler);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * This method is a simplified version of {@link io.unlogged.core.processor.UnloggedProcessor#getJavacProcessingEnvironment}
     * It simply returns the processing environment, but in case of gradle incremental compilation,
     * the delegate ProcessingEnvironment of the gradle wrapper is returned.
     */
    private static ProcessingEnvironment getJavacProcessingEnvironment(ProcessingEnvironment procEnv,
                                                                       List<String> delayedWarnings) {
        return tryRecursivelyObtainJavacProcessingEnvironment(procEnv);
    }

    private static ProcessingEnvironment tryRecursivelyObtainJavacProcessingEnvironment(ProcessingEnvironment procEnv) {
        if (procEnv.getClass().getName().equals("com.sun.tools.javac.processing.JavacProcessingEnvironment")) {
            return procEnv;
        }

        for (Class<?> procEnvClass = procEnv.getClass(); procEnvClass != null; procEnvClass = procEnvClass.getSuperclass()) {
            try {
                Object delegate = tryGetDelegateField(procEnvClass, procEnv);
                if (delegate == null) delegate = tryGetProcessingEnvField(procEnvClass, procEnv);
                if (delegate == null) delegate = tryGetProxyDelegateToField(procEnvClass, procEnv);

                if (delegate != null)
                    return tryRecursivelyObtainJavacProcessingEnvironment((ProcessingEnvironment) delegate);
            } catch (final Exception e) {
                // no valid delegate, try superclass
            }
        }

        return null;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!delayedWarnings.isEmpty()) {
            Set<? extends Element> rootElements = roundEnv.getRootElements();
            if (!rootElements.isEmpty()) {
                Element firstRoot = rootElements.iterator().next();
                for (String warning : delayedWarnings)
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, warning, firstRoot);
                delayedWarnings.clear();
            }
        }

        for (ProcessorDescriptor proc : active) proc.process(annotations, roundEnv);

        boolean onlyLombok = true;
        boolean zeroElems = true;
        for (TypeElement elem : annotations) {
            zeroElems = false;
            Name n = elem.getQualifiedName();
            if (n.toString().startsWith("io.unlogged")) continue;
            onlyLombok = false;
        }

        // Normally we rely on the claiming processor to claim away all lombok annotations.
        // One of the many Java9 oversights is that this 'process' API has not been fixed to address the point that 'files I want to look at' and 'annotations I want to claim' must be one and the same,
        // and yet in java9 you can no longer have 2 providers for the same service, thus, if you go by module path, lombok no longer loads the ClaimingProcessor.
        // This doesn't do as good a job, but it'll have to do. The only way to go from here, I think, is either 2 modules, or use reflection hackery to add ClaimingProcessor during our init.

        return onlyLombok && !zeroElems;
    }

    @Override
    public synchronized void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        for (ProcessorDescriptor proc : registered) {
            if (proc.want(procEnv, delayedWarnings)) active.add(proc);
        }

        if (active.isEmpty() && delayedWarnings.isEmpty()) {
            StringBuilder supported = new StringBuilder();
            for (ProcessorDescriptor proc : registered) {
                if (supported.length() > 0) supported.append(", ");
                supported.append(proc.getName());
            }
            if (procEnv.getClass().getName().equals("com.google.turbine.processing.TurbineProcessingEnvironment")) {
                procEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        String.format("Turbine is not currently supported by lombok."));
            } else {
                procEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format(
                        "You aren't using a compiler supported by lombok, so lombok will not work and has been disabled.\n" +
                                "Your processor is: %s\nLombok supports: %s", procEnv.getClass().getName(), supported));
            }
        }
    }

    static abstract class ProcessorDescriptor {
        abstract boolean want(ProcessingEnvironment procEnv, List<String> delayedWarnings);

        abstract String getName();

        abstract boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
    }

    static class JavacDescriptor extends ProcessorDescriptor {
        private Processor processor;

        @Override
        String getName() {
            return "OpenJDK javac";
        }

        @Override
        boolean want(ProcessingEnvironment procEnv, List<String> delayedWarnings) {
            // do not run on ECJ as it may print warnings
            if (procEnv.getClass().getName().startsWith("org.eclipse.jdt.")) return false;

            ProcessingEnvironment javacProcEnv = getJavacProcessingEnvironment(procEnv, delayedWarnings);

            if (javacProcEnv == null) return false;

            try {
                ClassLoader classLoader = findAndPatchClassLoader(javacProcEnv);
                processor = (Processor) Class.forName("io.unlogged.core.processor.UnloggedProcessor", false,
                                classLoader)
                        .getConstructor().newInstance();
            } catch (Exception e) {
                delayedWarnings.add(
                        "You found a bug in unlogged; io.unlogged.core.processor.UnloggedProcessor is not available. " +
                                "Unlogged will not run during this compilation: " + trace(
                                e));
                return false;
            } catch (NoClassDefFoundError e) {
                delayedWarnings.add(
                        "Can't load javac processor due to (most likely) a class loader problem: " + trace(e));
                return false;
            }
            try {
                processor.init(procEnv);
            } catch (Exception e) {
                e.printStackTrace();
                delayedWarnings.add(
                        "lombok.javac.apt.LombokProcessor could not be initialized. Lombok will not run during this compilation: " + trace(
                                e));
                return false;
            } catch (NoClassDefFoundError e) {
                delayedWarnings.add(
                        "Can't initialize javac processor due to (most likely) a class loader problem: " + trace(e));
                return false;
            }
            return true;
        }

        private ClassLoader findAndPatchClassLoader(ProcessingEnvironment procEnv) throws Exception {
            ClassLoader environmentClassLoader = procEnv.getClass().getClassLoader();
            if (environmentClassLoader != null && environmentClassLoader.getClass().getCanonicalName()
                    .equals("org.codehaus.plexus.compiler.javac.IsolatedClassLoader")) {
                if (!ClassLoader_lombokAlreadyAddedTo.getAndSet(environmentClassLoader, true)) {
                    Method m = Permit.getMethod(environmentClassLoader.getClass(), "addURL", URL.class);
                    URL selfUrl = new File(ClassRootFinder.findClassRootOfClass(AnnotationProcessor.class)).toURI()
                            .toURL();
                    Permit.invoke(m, environmentClassLoader, selfUrl);
                }
            }

            ClassLoader ourClassLoader = JavacDescriptor.class.getClassLoader();
            if (ourClassLoader == null) return ClassLoader.getSystemClassLoader();
            return ourClassLoader;
        }

        @Override
        boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return processor.process(annotations, roundEnv);
        }
    }
}
