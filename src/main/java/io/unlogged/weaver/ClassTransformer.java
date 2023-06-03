package io.unlogged.weaver;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

public class ClassTransformer {

    private final WeaveLog weavingInfo;
    private final JCTree.JCClassDecl jcClassDecl;
    private final TreeMaker treeMaker;
    private final JavacElements elementUtils;
    private WeaveConfig weaveConfig;

    public ClassTransformer(
            WeaveLog weavingInfo,
            WeaveConfig weaveConfig,
            JCTree.JCClassDecl jcClassDecl,
            TreeMaker treeMaker,
            JavacElements elementUtils
    ) {
        this.weavingInfo = weavingInfo;
        this.weaveConfig = weaveConfig;
        this.jcClassDecl = jcClassDecl;
        this.treeMaker = treeMaker;
        this.elementUtils = elementUtils;


        System.out.println("ClassElement: " + jcClassDecl.getSimpleName());
        List<JCTree> classMemberElements = jcClassDecl.getMembers();
        for (JCTree classMemberElement : classMemberElements) {
            System.out.println("\tMember [" + classMemberElement.getClass().getCanonicalName() + "]");
            if (classMemberElement instanceof JCTree.JCMethodDecl) {
                Name name = ((JCTree.JCMethodDecl) classMemberElement).getName();
                if (name.toString().equals("<init>")) {
                    continue;
                }
                System.out.println("Member name: " + name);
                MethodTransformer methodTransformer = new MethodTransformer(weavingInfo, weaveConfig,
                        (JCTree.JCMethodDecl) classMemberElement, treeMaker, elementUtils);
                System.out.println("Updated method: " + classMemberElement);
            }
        }

    }

}
