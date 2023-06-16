package io.unlogged.core.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.unlogged.Unlogged;
import io.unlogged.core.AnnotationValues;
import io.unlogged.javac.Javac;
import io.unlogged.javac.JavacAnnotationHandler;
import io.unlogged.javac.JavacNode;
import io.unlogged.javac.JavacTreeMaker;

import static io.unlogged.core.handlers.JavacHandlerUtil.*;

public class UnloggedAnnotationHandler extends JavacAnnotationHandler<Unlogged> {

    public static final String UNLOGGED_RUNTIME_FIELD_NAME = "unloggedRuntimeInstance";

    @Override
    public void handle(AnnotationValues<Unlogged> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        System.out.println("Handle annotation: " + Unlogged.class.getCanonicalName());

        JavacNode methodNode = annotationNode.up();
        JavacHandlerUtil.MemberExistsResult fieldExists = JavacHandlerUtil.fieldExists(UNLOGGED_RUNTIME_FIELD_NAME,
                methodNode);
        if (fieldExists != JavacHandlerUtil.MemberExistsResult.NOT_EXISTS) {
            annotationNode.addError("Field already exists: " + UNLOGGED_RUNTIME_FIELD_NAME);
            return;
        }
        JavacTreeMaker maker = methodNode.getTreeMaker();

        JCTree.JCExpression runtimeFieldType = chainDotsString(methodNode, "io.unlogged.Runtime");
        JCTree.JCExpression factoryMethod = chainDotsString(methodNode, "io.unlogged.Runtime.getInstance");
        JCTree.JCExpression[] factoryParameters = new JCTree.JCExpression[]{
                maker.Literal("")
        };

        JCTree.JCMethodInvocation factoryMethodCall = maker.Apply(
                List.<JCTree.JCExpression>nil(), factoryMethod, List.<JCTree.JCExpression>from(factoryParameters));

        JCTree.JCVariableDecl fieldDecl = recursiveSetGeneratedBy(maker.VarDef(
                maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC),
                methodNode.toName(UNLOGGED_RUNTIME_FIELD_NAME), runtimeFieldType, factoryMethodCall), annotationNode);

        JavacNode typeNode = upToTypeNode(methodNode);
        if (isRecord(typeNode) && Javac.getJavaCompilerVersion() < 16) {
            // This is a workaround for https://bugs.openjdk.java.net/browse/JDK-8243057
            injectField(typeNode, fieldDecl);
        } else {
            injectFieldAndMarkGenerated(typeNode, fieldDecl);
        }

    }
}
