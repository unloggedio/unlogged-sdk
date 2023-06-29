/*
 * Copyright (C) 2009-2018 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.unlogged.core.javac;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import io.unlogged.Unlogged;
import io.unlogged.core.CleanupRegistry;
import io.unlogged.core.TypeLibrary;
import io.unlogged.core.TypeResolver;
import io.unlogged.core.configuration.ConfigurationKeysLoader;
import io.unlogged.core.handlers.JavacHandlerUtil;
import io.unlogged.core.handlers.UnloggedAnnotationHandler;
import io.unlogged.weaver.TypeHierarchy;
import io.unlogged.weaver.UnloggedVisitor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import static io.unlogged.core.AnnotationValues.AnnotationValueDecodeFail;
import static io.unlogged.core.handlers.JavacHandlerUtil.chainDotsString;
import static io.unlogged.core.handlers.JavacHandlerUtil.recursiveSetGeneratedBy;

/**
 * This class tracks 'handlers' and knows how to invoke them for any given AST node.
 * <p>
 * This class can find the handlers (via SPI discovery) and will set up the given AST node, such as
 * building an AnnotationValues instance.
 */
public class HandlerLibrary {
    private static final TypeHierarchy typeHierarchy = new TypeHierarchy();
    private final TypeLibrary typeLibrary = new TypeLibrary();
    private final Map<String, List<AnnotationHandlerContainer<?>>> annotationHandlers = new HashMap<String, List<AnnotationHandlerContainer<?>>>();
    private final Collection<VisitorContainer> visitorHandlers = new ArrayList<VisitorContainer>();
    private final Messager messager;
    //    private final Weaver weaver;
    private final UnloggedVisitor unloggedVisitor;
    private final Trees trees;
    private SortedSet<Long> priorities;
    private SortedSet<Long> prioritiesRequiringResolutionReset;

    /**
     * Creates a new HandlerLibrary that will report any problems or errors to the provided messager.
     * You probably want to use {@link #load(Messager, Trees)} instead.
     */
    public HandlerLibrary(Messager messager, Trees trees) {
        ConfigurationKeysLoader.LoaderLoader.loadAllConfigurationKeys();
        this.messager = messager;
        this.trees = trees;
        unloggedVisitor = new UnloggedVisitor(typeHierarchy);
//        String agentArgs = "";
//        WeaveParameters weaveParameters = new WeaveParameters(agentArgs);
//        WeaveConfig weaveConfig = new WeaveConfig(weaveParameters);
//        this.weaver = new Weaver(new File("./session"), weaveConfig);
    }

    public static TypeHierarchy getTypeHierarchy() {
        return typeHierarchy;
    }

    /**
     * Creates a new HandlerLibrary that will report any problems or errors to the provided messager,
     * then uses SPI discovery to load all annotation and visitor based handlers so that future calls
     * to the handle methods will defer to these handlers.
     */
    public static HandlerLibrary load(Messager messager, Trees trees) {
        HandlerLibrary library = new HandlerLibrary(messager, trees);
        try {
            loadAnnotationHandlers(library, trees);
//            loadVisitorHandlers(library, trees);
        } catch (IOException e) {
            System.err.println("Unlogged isn't running due to misconfigured SPI files: " + e);
        }
//
//        library.calculatePriorities();

        return library;
    }

    /**
     * Uses SPI Discovery to find implementations of {@link JavacAnnotationHandler}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void loadAnnotationHandlers(HandlerLibrary lib, Trees trees) throws IOException {

        List<AnnotationHandlerContainer<?>> list = new ArrayList<>(1);
        AnnotationHandlerContainer<Unlogged> unloggedHandlerContainer =
                new AnnotationHandlerContainer<>(new UnloggedAnnotationHandler(), Unlogged.class);
        list.add(unloggedHandlerContainer);
        lib.annotationHandlers.put(Unlogged.class.getCanonicalName(), list);

        lib.typeLibrary.addType(unloggedHandlerContainer.annotationClass.getName());

        //No, that seemingly superfluous reference to JavacAnnotationHandler's classloader is not in fact superfluous!
//        for (JavacAnnotationHandler handler : SpiLoadUtil.findServices(JavacAnnotationHandler.class,
//                JavacAnnotationHandler.class.getClassLoader())) {
//            handler.setTrees(trees);
//            Class<? extends Annotation> annotationClass = handler.getAnnotationHandledByThisHandler();
//            AnnotationHandlerContainer<?> container = new AnnotationHandlerContainer(handler, annotationClass);
//            String annotationClassName = container.annotationClass.getName().replace("$", ".");
//            List<AnnotationHandlerContainer<?>> list = lib.annotationHandlers.get(annotationClassName);
//            if (list == null)
//                lib.annotationHandlers.put(annotationClassName, list = new ArrayList<AnnotationHandlerContainer<?>>(1));
//            list.add(container);
//            lib.typeLibrary.addType(container.annotationClass.getName());
//        }
    }

//    /**
//     * Uses SPI Discovery to find implementations of {@link JavacASTVisitor}.
//     */
//    private static void loadVisitorHandlers(HandlerLibrary lib, Trees trees) throws IOException {
//        //No, that seemingly superfluous reference to JavacASTVisitor's classloader is not in fact superfluous!
//        for (JavacASTVisitor visitor : SpiLoadUtil.findServices(JavacASTVisitor.class,
//                JavacASTVisitor.class.getClassLoader())) {
//            visitor.setTrees(trees);
//            lib.visitorHandlers.add(new VisitorContainer(visitor));
//        }
//    }

    public SortedSet<Long> getPriorities() {
        return priorities;
    }

    public SortedSet<Long> getPrioritiesRequiringResolutionReset() {
        return prioritiesRequiringResolutionReset;
    }

//    private void calculatePriorities() {
//        SortedSet<Long> set = new TreeSet<Long>();
//        SortedSet<Long> resetNeeded = new TreeSet<Long>();
//        for (List<AnnotationHandlerContainer<?>> containers : annotationHandlers.values()) {
//            for (AnnotationHandlerContainer<?> container : containers) {
//                set.add(container.getPriority());
//                if (container.isResolutionResetNeeded()) resetNeeded.add(container.getPriority());
//            }
//        }
//        for (VisitorContainer container : visitorHandlers) {
//            set.add(container.getPriority());
//            if (container.isResolutionResetNeeded()) resetNeeded.add(container.getPriority());
//        }
//        this.priorities = Collections.unmodifiableSortedSet(set);
//        this.prioritiesRequiringResolutionReset = Collections.unmodifiableSortedSet(resetNeeded);
//    }

    /**
     * Generates a warning in the Messenger that was used to initialize this HandlerLibrary.
     */
    public void javacWarning(String message) {
        javacWarning(message, null);
    }

    /**
     * Generates a warning in the Messager that was used to initialize this HandlerLibrary.
     */
    public void javacWarning(String message, Throwable t) {
        messager.printMessage(Diagnostic.Kind.WARNING, message + (t == null ? "" : (": " + t)));
    }

    /**
     * Generates an error in the Messager that was used to initialize this HandlerLibrary.
     */
    public void javacError(String message) {
        javacError(message, null);
    }

    /**
     * Generates an error in the Messager that was used to initialize this HandlerLibrary.
     */
    public void javacError(String message, Throwable t) {
        messager.printMessage(Diagnostic.Kind.ERROR, message + (t == null ? "" : (": " + t)));
        if (t != null) t.printStackTrace();
    }

    private boolean checkAndSetHandled(JCTree node) {
        return !JavacAugments.JCTree_handled.getAndSet(node, true);
    }

    /**
     * Handles the provided annotation node by first finding a qualifying instance of
     * {@link JavacAnnotationHandler} and if one exists, calling it with a freshly cooked up
     * instance of {@link io.unlogged.core.AnnotationValues}.
     * <p>
     *
     * @param unit       The Compilation Unit that contains the Annotation AST Node.
     * @param node       The AST Node representing the Annotation AST Node.
     * @param annotation 'node.get()' - convenience parameter.
     */
    public void handleAnnotation(JCCompilationUnit unit, JavacNode node, JCAnnotation annotation, long priority) {
//        if (1 < 2) {
//            return;
//        }
        TypeResolver resolver = new TypeResolver(node.getImportList());
        String rawType = annotation.annotationType.toString();
        String fqn = resolver.typeRefToFullyQualifiedName(node, typeLibrary, rawType);
        if (fqn == null) return;
        List<AnnotationHandlerContainer<?>> containers = annotationHandlers.get(fqn);
        if (containers == null) return;

        for (AnnotationHandlerContainer<?> container : containers) {
            try {
//                if (container.getPriority() == priority) {
                if (checkAndSetHandled(annotation)) {
                    container.handle(node);
                } else {
                    if (container.isEvenIfAlreadyHandled()) container.handle(node);
                }
//                }
            } catch (AnnotationValueDecodeFail fail) {
                fail.owner.setError(fail.getMessage(), fail.idx);
            } catch (Throwable t) {
                String sourceName = "(unknown).java";
                if (unit != null && unit.sourcefile != null) sourceName = unit.sourcefile.getName();
                javacError(String.format("Unlogged annotation handler %s failed on " + sourceName,
                        container.handler.getClass()), t);
            }
        }
    }

    /**
     * Will call all registered {@link JavacASTVisitor} instances.
     */
    public void callASTVisitors(JavacAST ast) {
        ast.traverse(unloggedVisitor);
    }

    public void finish(Messager messager, Context context, CleanupRegistry cleanup) {
        Base64.Encoder base64Encoder = Base64.getEncoder();
        Map<JCTree.JCClassDecl, ClassInfo> classRoots = unloggedVisitor.getClassRoots();
        Map<ClassInfo, JavacNode> classNodeMap = unloggedVisitor.getClassNodeMap();
        Map<ClassInfo, List<MethodInfo>> methodRoots = unloggedVisitor.getMethodMap();
        Map<ClassInfo, List<DataInfo>> dataInfoListMap = unloggedVisitor.getClassDataInfoList();
        Map<JCTree.JCClassDecl, JCCompilationUnit> compilationRoots = unloggedVisitor.getClassToCompilationRoots();

        for (Map.Entry<JCTree.JCClassDecl, ClassInfo> entry : classRoots.entrySet()) {
            JCTree.JCClassDecl classTree = entry.getKey();
            ClassInfo classInfo = entry.getValue();
//            JavacNode classNode = classNodeMap.get(classInfo);
            JCCompilationUnit compilationUnit = compilationRoots.get(classTree);

            JavacAST ast = new JavacAST(messager, context, compilationUnit, cleanup);
            JavacNode node = ast.get(classTree);

            List<Integer> probeIdsToRecord = new ArrayList<>();

            List<MethodInfo> methodInfoList = methodRoots.get(classInfo);
            List<DataInfo> dataInfoList = dataInfoListMap.get(classInfo);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                classInfo.writeToOutputStream(byteArrayOutputStream);
                for (DataInfo dataInfo : dataInfoList) {
                    dataInfo.writeToStream(byteArrayOutputStream);
                    if (dataInfo.getEventType().equals(EventType.METHOD_PARAM) || true) {
                        probeIdsToRecord.add(dataInfo.getDataId());
                    }
                }
                for (MethodInfo methodInfo : methodInfoList) {
                    methodInfo.writeToOutputStream(byteArrayOutputStream);
                }

            } catch (IOException e) {
                // throw new RuntimeException(e);
            }

            byte[] classWeaveBytes = byteArrayOutputStream.toByteArray();


            JavacTreeMaker maker = node.getTreeMaker();
            JCTree.JCExpression runtimeFieldType = chainDotsString(node, "java.lang.Boolean");
            JCTree.JCExpression factoryMethod = chainDotsString(node, "io.unlogged.Runtime.registerClass");

            JCTree.JCExpression arraysAsListMethod = chainDotsString(node, "java.util.Arrays.asList");
            JCTree.JCExpression[] probeIdsAsArrayExpression = new JCTree.JCExpression[probeIdsToRecord.size()];
            for (int i = 0; i < probeIdsToRecord.size(); i++) {
                Integer integer = probeIdsToRecord.get(i);
                probeIdsAsArrayExpression[i] = maker.Literal(integer);
            }

            JCTree.JCMethodInvocation functionCreatesProbeIdsArray = maker.Apply(
                    com.sun.tools.javac.util.List.<JCTree.JCExpression>nil(), arraysAsListMethod,
                    com.sun.tools.javac.util.List.<JCTree.JCExpression>from(probeIdsAsArrayExpression));

            String encodedClassBytes = base64Encoder.encodeToString(classWeaveBytes);
            if (encodedClassBytes.length() > 30000) {
                continue;
            }
            JCTree.JCExpression[] factoryParameters = new JCTree.JCExpression[]{
                    maker.Literal(encodedClassBytes),
                    functionCreatesProbeIdsArray
            };


            JCTree.JCMethodInvocation factoryMethodCall = maker.Apply(
                    com.sun.tools.javac.util.List.<JCTree.JCExpression>nil(), factoryMethod,
                    com.sun.tools.javac.util.List.<JCTree.JCExpression>from(factoryParameters));

            JCTree.JCVariableDecl fieldDecl = recursiveSetGeneratedBy(maker.VarDef(
                    maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC),
                    node.toName("unloggedClassLoaded"), runtimeFieldType, factoryMethodCall), node);


            JavacHandlerUtil.injectField(node, fieldDecl);
            System.out.println("Class [" + classInfo.getClassName() + "] ->\n" + node.get()
                    + "\n=======================================================================\n");
        }

        // todo
    }

    private static class VisitorContainer {
        private final JavacASTVisitor visitor;
        private final long priority;
        private final boolean resolutionResetNeeded;

        VisitorContainer(JavacASTVisitor visitor) {
            this.visitor = visitor;
//			HandlerPriority hp = visitor.getClass().getAnnotation(HandlerPriority.class);
            this.priority = 0L;
            this.resolutionResetNeeded = false; //visitor.getClass().isAnnotationPresent(ResolutionResetNeeded.class);
        }

        public long getPriority() {
            return priority;
        }

        public boolean isResolutionResetNeeded() {
            return resolutionResetNeeded;
        }
    }

    private static class AnnotationHandlerContainer<T extends Annotation> {
        private final JavacAnnotationHandler<T> handler;
        private final Class<T> annotationClass;
        private final long priority;
        private final boolean resolutionResetNeeded;
        private final boolean evenIfAlreadyHandled;

        AnnotationHandlerContainer(JavacAnnotationHandler<T> handler, Class<T> annotationClass) {
            this.handler = handler;
            this.annotationClass = annotationClass;
//			HandlerPriority hp = handler.getClass().getAnnotation(HandlerPriority.class);
            this.priority = 0L; //hp == null ? 0L : (((long) hp.value()) << 32) + hp.subValue();
            this.resolutionResetNeeded = false; // handler.getClass().isAnnotationPresent(ResolutionResetNeeded.class);
            this.evenIfAlreadyHandled = false; //handler.getClass().isAnnotationPresent(AlreadyHandledAnnotations
            // .class);
        }

        public void handle(final JavacNode node) {
            handler.handle(JavacHandlerUtil.createAnnotation(annotationClass, node), (JCAnnotation) node.get(), node);
        }

        public long getPriority() {
            return priority;
        }

        public boolean isResolutionResetNeeded() {
            return resolutionResetNeeded;
        }

        public boolean isEvenIfAlreadyHandled() {
            return evenIfAlreadyHandled;
        }
    }
}
