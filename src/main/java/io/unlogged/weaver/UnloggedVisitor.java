package io.unlogged.weaver;

import com.insidious.common.weaver.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.unlogged.core.ImportList;
import io.unlogged.core.TypeLibrary;
import io.unlogged.core.TypeResolver;
import io.unlogged.core.handlers.JavacHandlerUtil;
import io.unlogged.core.javac.JavacASTAdapter;
import io.unlogged.core.javac.JavacNode;
import io.unlogged.core.javac.JavacTreeMaker;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnloggedVisitor extends JavacASTAdapter {

    private final Map<String, Boolean> methodRoots = new HashMap<>();
    private final Map<JCTree, ClassInfo> classRoots = new HashMap<>();
    private final Map<ClassInfo, JavacNode> classJavacNodeMap = new HashMap<>();
    private final Map<ClassInfo, java.util.List<MethodInfo>> classMethodInfoList = new HashMap<>();
    private final Map<ClassInfo, java.util.List<DataInfo>> classDataInfoList = new HashMap<>();
    private final DataInfoProvider dataInfoProvider;
    private final TypeLibrary typeLibrary = new TypeLibrary();

    public UnloggedVisitor(DataInfoProvider dataInfoProvider) {
        this.dataInfoProvider = dataInfoProvider;
    }

    @Override
    public void visitCompilationUnit(JavacNode top, JCTree.JCCompilationUnit unit) {
        for (JCTree.JCImport anImport : unit.getImports()) {
            typeLibrary.addType(anImport.getQualifiedIdentifier().toString());
        }

    }

    @Override
    public void visitMethod(JavacNode methodNode, JCTree.JCMethodDecl jcMethodDecl) {
        JavacNode classNode = methodNode.up();
        if (!JavacHandlerUtil.isClass(classNode)) {
            // no probing for interfaces and enums
            return;
        }
        JCTree classElement = classNode.get();
        String packageDeclaration = classNode.getPackageDeclaration();
        String className = classNode.getName();
        if (packageDeclaration != null) {
            className = packageDeclaration + "." + className;
        }
        List<JCTree.JCVariableDecl> methodParameters = jcMethodDecl.params;
        String methodDoneKey = className + jcMethodDecl.getName() + methodParameters.toString();

        ClassInfo classInfo;
        if (!classRoots.containsKey(classElement)) {

            ImportList importList = classNode.getImportList();
            TypeResolver resolver = new TypeResolver(importList);

            JCTree.JCClassDecl classDeclaration = (JCTree.JCClassDecl) classNode.get();
            Element element = classNode.getElement();
            String superClassFQN = "";
            if (classDeclaration.getExtendsClause() != null) {
                String superClassName = ((JCTree.JCIdent) classDeclaration.getExtendsClause()).getName().toString();
                superClassFQN = resolver.typeRefToFullyQualifiedName(classNode, typeLibrary, superClassName);
                if (superClassFQN == null && packageDeclaration != null) {
                    superClassFQN = packageDeclaration + "." + superClassName;
                }
            }
            String[] interfaces = {};
            List<JCTree.JCExpression> interfaceClasses = classDeclaration.getImplementsClause();
            if (interfaceClasses != null) {
                interfaces = new String[interfaceClasses.size()];
                for (int i = 0; i < interfaceClasses.size(); i++) {
                    JCTree.JCIdent interfaceClause = (JCTree.JCIdent) interfaceClasses.get(i);
                    String interfaceClassName = interfaceClause.getName().toString();
                    String interfaceClassFQN = resolver.typeRefToFullyQualifiedName(classNode, typeLibrary,
                            interfaceClassName);
                    if (interfaceClassFQN == null && packageDeclaration != null) {
                        interfaceClassFQN = packageDeclaration + "." + interfaceClassName;
                    }
                    interfaces[i] = interfaceClassFQN;
                }
            }

            classInfo = new ClassInfo(
                    dataInfoProvider.nextClassId(), "classContainer", classNode.up().getFileName(),
                    className, LogLevel.Normal, String.valueOf(element.hashCode()),
                    "classLoader", interfaces, superClassFQN, "signature");
            classRoots.put(classElement, classInfo);
            classDataInfoList.put(classInfo, new ArrayList<>());
            classMethodInfoList.put(classInfo, new ArrayList<>());
        } else {
            classInfo = classRoots.get(classElement);
        }
        classJavacNodeMap.put(classInfo, classNode);
        if (methodNode.isStatic()) {
            return;
        }

        if (methodRoots.containsKey(methodDoneKey)) {
            return;
        }
        String methodName = methodNode.getName();
        String methodDesc = createMethodDescriptor(methodNode);
        MethodInfo methodInfo = new MethodInfo(classInfo.getClassId(), dataInfoProvider.nextMethodId(),
                className, methodName, methodDesc, (int) jcMethodDecl.mods.flags,
                "sourceFileName", String.valueOf(jcMethodDecl.hashCode()));
        classMethodInfoList.get(classInfo).add(methodInfo);
        methodRoots.put(methodDoneKey, true);


        String methodSignature = methodParameters.toString();


        DataInfo dataInfo = new DataInfo(classInfo.getClassId(), methodInfo.getMethodId(),
                dataInfoProvider.nextProbeId(), methodNode.getStartPos(), 0,
                EventType.METHOD_ENTRY, Descriptor.Void, "");

        classDataInfoList.get(classInfo).add(dataInfo);

        ListBuffer<JCTree.JCStatement> parameterProbes = new ListBuffer<JCTree.JCStatement>();
        for (JCTree.JCVariableDecl methodParameter : methodParameters) {
            DataInfo paramProbeDataInfo = new DataInfo(classInfo.getClassId(), methodInfo.getMethodId(),
                    dataInfoProvider.nextProbeId(), methodNode.getStartPos(), 0,
                    EventType.METHOD_PARAM, Descriptor.get(methodParameter.getType().toString()), "");
            JCTree.JCExpressionStatement paramProbe = createLogStatement(methodNode,
                    methodParameter.getName().toString(), paramProbeDataInfo.getDataId());
            parameterProbes.add(paramProbe);
        }


        System.out.println("Visit method: " + className + "." + methodName + "( " + methodSignature + "  )");
        JavacTreeMaker treeMaker = methodNode.getTreeMaker();

        JCTree.JCExpressionStatement logStatement = createLogStatement(methodNode, "this", dataInfo.getDataId());

        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<JCTree.JCStatement>();

        JCTree.JCBlock methodBodyBlock = jcMethodDecl.body;
        if (methodBodyBlock == null) {
            // no probes for empty method
            // maybe we fill it up later
            methodBodyBlock = treeMaker.Block(0, List.nil());
//            return;
        }

        List<JCTree.JCStatement> blockStatements = methodBodyBlock.getStatements();

        boolean foundSuperCall = false;
        if (!methodName.equals("<init>")) {
            foundSuperCall = true;
            newStatements.add(logStatement);
            newStatements.addAll(parameterProbes);
            newStatements.addAll(blockStatements);
        } else {
            for (JCTree.JCStatement statement : blockStatements) {
                newStatements.add(statement);
                if (!foundSuperCall) {
                    if (statement instanceof JCTree.JCExpressionStatement) {
                        JCTree.JCExpressionStatement expressionStatement = (JCTree.JCExpressionStatement) statement;
                        JCTree.JCExpression expression = expressionStatement.getExpression();
                        if (expression instanceof JCTree.JCMethodInvocation) {
                            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) expression;
                            JCTree.JCExpression methodSelect = methodInvocation.getMethodSelect();
                            if (methodSelect instanceof JCTree.JCIdent) {
                                JCTree.JCIdent methodNameIdentifier = (JCTree.JCIdent) methodSelect;
                                if ("super".equals(methodNameIdentifier.getName().toString())) {
                                    foundSuperCall = true;
                                    newStatements.add(logStatement);
                                    newStatements.addAll(parameterProbes);
                                }
                            }
                        }
                    }
                }
            }
            if (!foundSuperCall) {
                newStatements.add(logStatement);
            }
        }




        jcMethodDecl.body = treeMaker.Block(0, newStatements.toList());
        System.out.println("After: " + jcMethodDecl.body);

    }

    private JCTree.JCExpressionStatement createLogStatement(JavacNode methodNode, String variableToRecord, int dataId) {
        JavacTreeMaker treeMaker = methodNode.getTreeMaker();
        JCTree.JCExpression printlnMethod = JavacHandlerUtil.chainDotsString(methodNode,
                "io.unlogged.logging.Logging.recordEvent");

        JCTree.JCExpressionStatement logStatement = treeMaker.Exec(
                treeMaker.Apply(
                        List.<JCTree.JCExpression>nil(),
                        printlnMethod,
                        List.<JCTree.JCExpression>of(
                                treeMaker.Ident(methodNode.toName(variableToRecord)),
                                treeMaker.Literal(dataId)
                        )
                )
        );
        return logStatement;
    }

    private String createMethodDescriptor(JavacNode methodNode) {
        return "(IL)Ljava.lang.Integer;";
    }

    public Map<JCTree, ClassInfo> getClassRoots() {
        return classRoots;
    }

    public Map<ClassInfo, JavacNode> getClassNodeMap() {
        return classJavacNodeMap;
    }

    public Map<ClassInfo, java.util.List<MethodInfo>> getMethodMap() {
        return classMethodInfoList;
    }

    public Map<ClassInfo, java.util.List<DataInfo>> getClassDataInfoList() {
        return classDataInfoList;
    }
}
