package io.unlogged;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import io.unlogged.weaver.WeaveParameters;
import io.unlogged.weaver.WeaveConfig;
import io.unlogged.weaver.Weaver;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @version 1.0
 */
@SupportedAnnotationTypes("io.unlogged.Unlogged")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UnloggedProcessor extends AbstractProcessor {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private ProcessingEnvironment unwrappedProcessingEnv;
    private Context context;
    private JavacElements elementUtils;
    private TreeMaker treeMaker;
    private Weaver weaver;

    public UnloggedProcessor() {
        System.out.println("HelloUnloggedProcessor");
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

    private void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        unwrappedProcessingEnv = jbUnwrap(ProcessingEnvironment.class, processingEnv);
        context = ((JavacProcessingEnvironment) unwrappedProcessingEnv).getContext();
        elementUtils = (JavacElements) processingEnv.getElementUtils();
        treeMaker = TreeMaker.instance(context);

        String agentArgs = "";
        System.out.println(
                "[unlogged] Starting agent: [" + Constants.AGENT_VERSION + "] with arguments [" + agentArgs + "]");
        synchronized (initialized) {
            if (initialized.get()) {
                return;
            }
            initialized.set(true);
        }


        WeaveParameters weaveParameters = new WeaveParameters(agentArgs);
        WeaveConfig weaveConfig = new WeaveConfig(weaveParameters);
        weaver = new Weaver(new File("./session"), weaveConfig, treeMaker, elementUtils);

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Unlogged.class);
        if (elements == null || elements.size() == 0) {
            return true;
        }

        Set<? extends Element> rootElements = roundEnv.getRootElements();
        for (Element classElement : rootElements) {
            JCTree elementTree = elementUtils.getTree(classElement);
            if (elementTree instanceof JCTree.JCClassDecl) {
                JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) elementTree;
                weaver.weave(classTree);
            }
        }
        return true;
    }

}
