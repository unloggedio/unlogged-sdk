package io.unlogged.weaver;

import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

public class MethodTransformer extends JCTree.Visitor {

    public static final String LOGGER_CLASS = "com/videobug/agent/logging/Logging";

    public static final String METHOD_RECORD_EVENT = "recordEvent";

    private final JCTree.JCMethodDecl jcMethodDecl;
    private final WeaveLog weavingInfo;
    private final WeaveConfig weaveConfig;
    private TreeMaker treeMaker;
    private JavacElements elementUtils;
    //    private int currentLine = 0;
    private int instructionIndex = 0;

    public MethodTransformer(
            WeaveLog weavingInfo,
            WeaveConfig config,
            JCTree.JCMethodDecl jcMethodDecl,
            TreeMaker treeMaker,
            JavacElements elementUtils
    ) {
        this.jcMethodDecl = jcMethodDecl;
        this.weavingInfo = weavingInfo;
        this.treeMaker = treeMaker;
        this.weaveConfig = config;
        this.elementUtils = elementUtils;

        String methodName = jcMethodDecl.getName().toString();

        addMethodEnterLog(jcMethodDecl);


    }

    private int nextDataId(EventType eventType, Descriptor desc, String label, int currentLine) {
        return weavingInfo.nextDataId(currentLine, instructionIndex, eventType, desc, label);
    }

//    /**
//     * Generate logging instructions.
//     *
//     * @param eventType specifies a data name.
//     * @param valueDesc specifies a data type.  If it has no data, use Descriptor.Void.
//     * @param label     specifies the label for that line
//     * @return dataId.
//     */
//    private int generateLogging(EventType eventType, Descriptor valueDesc, String label, JCTree jcTree) {
//        int dataId = nextDataId(eventType, valueDesc, label, jcTree.pos);
//        super.visitLdcInsn(dataId);
//        if (valueDesc == Descriptor.Void) {
//            super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT, "(I)V", false);
//        } else {
//            super.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_CLASS, METHOD_RECORD_EVENT,
//                    "(" + valueDesc.getString() + "I)V", false);
//        }
//        return dataId;
//    }

    private void addMethodEnterLog(JCTree.JCMethodDecl jcMethodDecl) {

        treeMaker.pos = jcMethodDecl.pos;
        JCTree.JCFieldAccess printlnMethod = createSystemOutPrintln();

        JCTree.JCBlock methodBodyBlock = jcMethodDecl.body;

//        jcMethodDecl.accept(this);

        for (JCTree.JCStatement statement : methodBodyBlock.getStatements()) {
            System.out.println("\t\tStatement: " + statement);
        }


        jcMethodDecl.body = treeMaker.Block(0, List.of(
                        treeMaker.Exec(
                                treeMaker.Apply(
                                        List.<JCTree.JCExpression>nil(),
                                        printlnMethod,
                                        List.<JCTree.JCExpression>of(
                                                treeMaker.Literal(
                                                        "Method " + jcMethodDecl.getName())
                                        )
                                )
                        ),
                        methodBodyBlock
                )
        );
    }

    private JCTree.JCFieldAccess createSystemOutPrintln() {
        JCTree.JCFieldAccess printlnMethod = treeMaker.Select(
                treeMaker.Select(
                        treeMaker.Ident(
                                elementUtils.getName("System")
                        ),
                        elementUtils.getName("out")
                ),
                elementUtils.getName("println")
        );
        return printlnMethod;
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation that) {
        treeMaker.pos = that.pos;

        super.visitApply(that);
    }
}
