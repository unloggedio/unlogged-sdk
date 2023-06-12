package io.unlogged.weaver;

import com.insidious.common.weaver.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
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

    public UnloggedVisitor(DataInfoProvider dataInfoProvider) {
        this.dataInfoProvider = dataInfoProvider;
    }

    @Override
    public void visitMethod(JavacNode methodNode, JCTree.JCMethodDecl jcMethodDecl) {
        JavacNode classNode = methodNode.up();
        JCTree classElement = classNode.get();
        String className = classNode.getName();
        String methodDoneKey = className + jcMethodDecl.getName() + jcMethodDecl.params.toString();

        ClassInfo classInfo;
        if (!classRoots.containsKey(classElement)) {
            Element element = classNode.getElement();
            classInfo = new ClassInfo(
                    dataInfoProvider.nextClassId(), "classContainer", classNode.up().getFileName(),
                    className, LogLevel.Normal, String.valueOf(element.hashCode()),
                    "classLoader", new String[]{}, "superName", "signature");
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


        String methodSignature = jcMethodDecl.params.toString();


        DataInfo dataInfo = new DataInfo(classInfo.getClassId(), methodInfo.getMethodId(),
                dataInfoProvider.nextProbeId(), methodNode.getStartPos(), 0,
                EventType.METHOD_ENTRY, Descriptor.Void, "");

        classDataInfoList.get(classInfo).add(dataInfo);

        System.out.println("Visit method: " + className + "." + methodName + "( " + methodSignature + "  )");
        JavacTreeMaker treeMaker = methodNode.getTreeMaker();

        JCTree.JCExpressionStatement logStatement = createLogStatement(methodNode, "this", dataInfo.getDataId());

        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<JCTree.JCStatement>();

        JCTree.JCBlock methodBodyBlock = jcMethodDecl.body;

        List<JCTree.JCStatement> blockStatements = methodBodyBlock.getStatements();

        if (!methodName.equals("<init>")) {
            newStatements.add(logStatement);
            newStatements.addAll(blockStatements);
        } else {
            boolean foundSuperCall = false;
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
