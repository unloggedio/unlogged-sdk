package io.unlogged.core.handlers;

import com.sun.tools.javac.tree.JCTree;
import io.unlogged.Unlogged;
import io.unlogged.core.AnnotationValues;
import io.unlogged.core.javac.JavacAnnotationHandler;
import io.unlogged.core.javac.JavacNode;

public class UnloggedAnnotationHandler extends JavacAnnotationHandler<Unlogged> {

    public static final String UNLOGGED_RUNTIME_FIELD_NAME = "unloggedRuntimeInstance";

    @Override
    public void handle(AnnotationValues<Unlogged> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        System.out.println("Handle annotation: " + Unlogged.class.getCanonicalName());

        JavacNode typeNode = annotationNode.up();
        JavacHandlerUtil.MemberExistsResult fieldExists = JavacHandlerUtil.fieldExists(UNLOGGED_RUNTIME_FIELD_NAME,
                typeNode);
        if (fieldExists != JavacHandlerUtil.MemberExistsResult.NOT_EXISTS) {
            annotationNode.addError("Field already exists: " + UNLOGGED_RUNTIME_FIELD_NAME);
            return;
        }

        // JavacHandlerUtil.injectFieldAndMarkGenerated()
    }
}
