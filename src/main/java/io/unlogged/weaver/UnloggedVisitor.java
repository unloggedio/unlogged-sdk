package io.unlogged.weaver;

import com.insidious.common.weaver.DataInfo;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import io.unlogged.core.javac.JavacASTAdapter;
import io.unlogged.core.javac.JavacNode;
import io.unlogged.core.javac.JavacTreeMaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UnloggedVisitor extends JavacASTAdapter {

    private final Map<JCTree.JCMethodDecl, java.util.List<DataInfo>> methodRoots = new HashMap<>();

    public UnloggedVisitor() {
    }

    @Override
    public void visitMethod(JavacNode methodNode, JCTree.JCMethodDecl jcMethodDecl) {
        if (methodRoots.containsKey(jcMethodDecl)) {
            return;
        }
        methodRoots.put(jcMethodDecl, new ArrayList<DataInfo>());

        String className = methodNode.up().getName();
        String methodName = methodNode.getName();
        String methodSignature = jcMethodDecl.params.toString();
        JCTree returnType1 = jcMethodDecl.getReturnType();
        String returnType = returnType1 != null ? returnType1.toString() : "";

        System.out.println("Visit method: " + className + "." + methodName + "( " + methodSignature + "  )");
        JavacTreeMaker treeMaker = methodNode.getTreeMaker();

        JCTree.JCFieldAccess printlnMethod = treeMaker.Select(
                treeMaker.Select(
                        treeMaker.Ident(
                                methodNode.toName("System")
                        ),
                        methodNode.toName("out")
                ),
                methodNode.toName("println")
        );

        JCTree.JCBlock methodBodyBlock = jcMethodDecl.body;

        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<JCTree.JCStatement>();
        JCTree.JCExpressionStatement logStatement = treeMaker.Exec(
                treeMaker.Apply(
                        List.<JCTree.JCExpression>nil(),
                        printlnMethod,
                        List.<JCTree.JCExpression>of(
                                treeMaker.Literal(
                                        "Method " + className + "." + methodName + "()")
                        )
                )
        );

        if (!methodName.equals("<init>")) {
            newStatements.add(logStatement);
            newStatements.addAll(methodBodyBlock.getStatements());
        } else {
            boolean foundSuperCall = false;
            for (JCTree.JCStatement statement : methodBodyBlock.getStatements()) {
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
//                System.out.println("\t\tStatement: " + statement);
            }
            if (!foundSuperCall) {
                newStatements.add(logStatement);
//                System.out.println("Did not find a super() call in method: " + methodName + " for class " + className);
            }
        }

        jcMethodDecl.body = treeMaker.Block(0, newStatements.toList());
        System.out.println("After: " + jcMethodDecl.body);

    }
}
