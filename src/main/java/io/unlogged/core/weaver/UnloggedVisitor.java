package io.unlogged.core.weaver;

import com.insidious.common.weaver.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import io.unlogged.core.TypeLibrary;
import io.unlogged.core.TypeResolver;
import io.unlogged.core.handlers.JavacHandlerUtil;
import io.unlogged.core.javac.JavacASTAdapter;
import io.unlogged.core.javac.JavacNode;
import io.unlogged.core.javac.JavacTreeMaker;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class UnloggedVisitor extends JavacASTAdapter {
    private final Map<String, Boolean> methodRoots = new HashMap<>();
    private final Map<JCTree.JCClassDecl, ClassInfo> classRoots = new HashMap<>();
    private final Map<JCTree.JCClassDecl, JCTree.JCCompilationUnit> classToCompilationRoots = new HashMap<>();
    private final Map<ClassInfo, JavacNode> classJavacNodeMap = new HashMap<>();
    private final Map<ClassInfo, java.util.List<MethodInfo>> classMethodInfoList = new HashMap<>();
    private final Map<ClassInfo, java.util.List<DataInfo>> classDataInfoList = new HashMap<>();
    private final DataInfoProvider dataInfoProvider;
    private final TypeLibrary typeLibrary = new TypeLibrary();
    private final Random random = new Random();
    private final TypeHierarchy typeHierarchy;
    private int currentClassId;
    private int currentMethodId;
    private int currentLineNumber;

    public UnloggedVisitor(DataInfoProvider dataInfoProvider, TypeHierarchy typeHierarchy) {
        this.dataInfoProvider = dataInfoProvider;
        this.typeHierarchy = typeHierarchy;
    }

    @Override
    public void visitType(JavacNode typeNode, JCTree.JCClassDecl type) {
        TypeResolver typeResolver = new TypeResolver(typeNode.top().getImportList());
        List<JCTree.JCExpression> interfaceClauseList = type.getImplementsClause();
        String[] interfaceNames = new String[0];
        if (interfaceClauseList != null && interfaceClauseList.size() > 0) {
            interfaceNames = new String[interfaceClauseList.size()];
            for (int i = 0; i < interfaceClauseList.size(); i++) {
                JCTree.JCExpression jcExpression = interfaceClauseList.get(i);
                String interfaceName = getQualifiedTypeName(typeNode, typeResolver, jcExpression);
                interfaceNames[i] = interfaceName;
            }
        }
        String superClassName = "";
        if (type.getExtendsClause() != null) {
            superClassName = getQualifiedTypeName(typeNode, typeResolver, type.getExtendsClause());
        }
        String typeName = typeNode.getName();
        String qualifiedTypeName = qualifiedNameFromString(typeNode, typeResolver, typeName);
        typeHierarchy.registerClass(qualifiedTypeName, superClassName, interfaceNames);
        super.visitType(typeNode, type);
    }

    @NotNull
    private String getQualifiedTypeName(JavacNode typeNode, TypeResolver typeResolver,
                                        JCTree.JCExpression typeExpression) {

        String typeName = typeExpression.toString();

        if (typeExpression instanceof JCTree.JCTypeApply) {
            JCTree.JCTypeApply appliedType = (JCTree.JCTypeApply) typeExpression;
            typeName = appliedType.getType().toString();
        } else if (typeExpression instanceof JCTree.JCIdent) {

        } else {
            System.err.println("Expression if not a JCTypeApply: " + typeExpression.getClass());
        }


        return qualifiedNameFromString(typeNode, typeResolver, typeName);
    }

    @NotNull
    private String qualifiedNameFromString(JavacNode typeNode, TypeResolver typeResolver, String typeName) {
        String qualifiedTypeName = typeResolver.typeRefToFullyQualifiedName(typeNode, typeLibrary, typeName);
        if (qualifiedTypeName == null) {
            qualifiedTypeName = typeNode.getPackageDeclaration() + "." + typeName;
        }
        return qualifiedTypeName;
    }

    @Override
    public void visitCompilationUnit(JavacNode top, JCTree.JCCompilationUnit unit) {
        for (JCTree.JCImport anImport : unit.getImports()) {
            typeLibrary.addType(anImport.getQualifiedIdentifier().toString());
        }

    }

    public Map<JCTree.JCClassDecl, JCTree.JCCompilationUnit> getClassToCompilationRoots() {
        return classToCompilationRoots;
    }

    @Override
    public void visitMethod(JavacNode methodNode, JCTree.JCMethodDecl jcMethodDecl) {
////        if (1 < 2) {
////            return;
////        }
//
//        JavacNode classNode = methodNode.up();
//        if (!JavacHandlerUtil.isClassAndDoesNotHaveFlags(classNode,
//                Flags.INTERFACE | Flags.ENUM | Flags.ANNOTATION | RECORD | Flags.ABSTRACT)) {
//            // no probing for interfaces and enums
//            return;
//        }
//        if (classNode.up().getKind() == TYPE) {
//            return;
//        }
//        JCTree.JCClassDecl classElement = (JCTree.JCClassDecl) classNode.get();
//        String packageDeclaration = classNode.getPackageDeclaration();
//        String className = classNode.getName();
//        if (packageDeclaration != null) {
//            className = packageDeclaration + "." + className;
//        }
//        List<JCTree.JCVariableDecl> methodParameters = jcMethodDecl.params;
//        String methodDoneKey = className + jcMethodDecl.getName() + methodParameters.toString();
//
//        ClassInfo classInfo;
//        if (!classRoots.containsKey(classElement)) {
//
//            ImportList importList = classNode.getImportList();
////            TypeResolver resolver = new TypeResolver(importList);
//
//            JCTree.JCClassDecl classDeclaration = (JCTree.JCClassDecl) classNode.get();
//            Element element = classNode.getElement();
//            if (element == null) {
//                return;
//            }
//            String superClassFQN = "";
////            if (classDeclaration.getExtendsClause() != null) {
////                String superClassName = ((JCTree.JCIdent) classDeclaration.getExtendsClause()).getName().toString();
////                superClassFQN = resolver.typeRefToFullyQualifiedName(classNode, typeLibrary, superClassName);
////                if (superClassFQN == null && packageDeclaration != null) {
////                    superClassFQN = packageDeclaration + "." + superClassName;
////                }
////            }
//            String[] interfaces = {};
//            List<JCTree.JCExpression> interfaceClasses = classDeclaration.getImplementsClause();
////            if (interfaceClasses != null) {
////                interfaces = new String[interfaceClasses.size()];
////                for (int i = 0; i < interfaceClasses.size(); i++) {
////                    JCTree.JCIdent interfaceClause = (JCTree.JCIdent) interfaceClasses.get(i);
////                    String interfaceClassName = interfaceClause.getName().toString();
////                    String interfaceClassFQN = resolver.typeRefToFullyQualifiedName(classNode, typeLibrary,
////                            interfaceClassName);
////                    if (interfaceClassFQN == null && packageDeclaration != null) {
////                        interfaceClassFQN = packageDeclaration + "." + interfaceClassName;
////                    }
////                    interfaces[i] = interfaceClassFQN;
////                }
////            }
//
//            classInfo = new ClassInfo(
//                    dataInfoProvider.nextClassId(), "classContainer", classNode.up().getFileName(),
//                    className, LogLevel.Normal, String.valueOf(element.hashCode()),
//                    "classLoader", interfaces, superClassFQN, "signature");
//            classRoots.put(classElement, classInfo);
//            classToCompilationRoots.put(classElement, (JCTree.JCCompilationUnit) classNode.top().get());
//            classDataInfoList.put(classInfo, new ArrayList<>());
//            classMethodInfoList.put(classInfo, new ArrayList<>());
//        } else {
//            classInfo = classRoots.get(classElement);
//        }
//        classJavacNodeMap.put(classInfo, classNode);
//        if (methodNode.isStatic()) {
//            return;
//        }
//
//        if (methodRoots.containsKey(methodDoneKey)) {
//            return;
//        }
//        JavacTreeMaker treeMaker = methodNode.getTreeMaker();
//
//        String methodName = methodNode.getName();
//        String methodDesc = createMethodDescriptor(methodNode);
//        MethodInfo methodInfo = new MethodInfo(classInfo.getClassId(), dataInfoProvider.nextMethodId(),
//                className, methodName, methodDesc, (int) jcMethodDecl.mods.flags,
//                "sourceFileName", String.valueOf(jcMethodDecl.hashCode()));
//        currentClassId = classInfo.getClassId();
//        currentMethodId = methodInfo.getMethodId();
//
//        classMethodInfoList.get(classInfo).add(methodInfo);
//        methodRoots.put(methodDoneKey, true);
//        java.util.List<DataInfo> dataInfoList = classDataInfoList.get(classInfo);
//
//
//        String methodSignature = methodParameters.toString();
//
//        currentLineNumber = getPositionToLine(classNode, methodNode.getStartPos());
//
//        DataInfo dataInfo = new DataInfo(classInfo.getClassId(), methodInfo.getMethodId(),
//                dataInfoProvider.nextProbeId(), currentLineNumber, 0,
//                EventType.METHOD_ENTRY, Descriptor.Void, "");
//
//        dataInfoList.add(dataInfo);
//
//        ListBuffer<JCTree.JCStatement> parameterProbes = new ListBuffer<JCTree.JCStatement>();
//        for (JCTree.JCVariableDecl methodParameter : methodParameters) {
//            JCTree methodParameterType = methodParameter.getType();
//
//            String methodParameterTypeFQN = "void";
//            if (methodParameterType instanceof JCTree.JCIdent) {
//                // todo delete this
//                if (((JCTree.JCIdent) methodParameterType).sym != null) {
//                    methodParameterTypeFQN = ((JCTree.JCIdent) methodParameterType).sym.getQualifiedName().toString();
//                }
//            } else if (methodParameterType instanceof JCTree.JCPrimitiveTypeTree) {
//                JCTree.JCPrimitiveTypeTree primitiveType = (JCTree.JCPrimitiveTypeTree) methodParameterType;
//                methodParameterTypeFQN = primitiveType.toString();
//            }
//
//            DataInfo paramProbeDataInfo = new DataInfo(classInfo.getClassId(), methodInfo.getMethodId(),
//                    dataInfoProvider.nextProbeId(), currentLineNumber, 0,
//                    EventType.METHOD_PARAM, Descriptor.get(methodParameterTypeFQN), "");
//            dataInfoList.add(paramProbeDataInfo);
//            String parameterName = methodParameter.getName().toString();
//            JCTree.JCExpressionStatement paramProbe =
//                    createLogStatement(methodNode, treeMaker.Ident(methodNode.toName(parameterName)),
//                            paramProbeDataInfo.getDataId());
//            parameterProbes.add(paramProbe);
//        }
//
//
//        System.out.println("Visit method: " + className + "." + methodName + "( " + methodSignature + "  )");
//
//        JCTree.JCExpressionStatement logStatement =
//                createLogStatement(methodNode, treeMaker.Ident(methodNode.toName("this")), dataInfo.getDataId());
//
//        ListBuffer<JCTree.JCStatement> newStatements = new ListBuffer<JCTree.JCStatement>();
//
//        JCTree.JCBlock methodBodyBlock = jcMethodDecl.body;
//        if (methodBodyBlock == null) {
//            // no probes for empty method
//            // maybe we fill it up later
//            methodBodyBlock = treeMaker.Block(0, List.nil());
////            return;
//        }
//
//        List<JCTree.JCStatement> blockStatements = methodBodyBlock.getStatements();
//
//        boolean foundSuperCall = false;
//        if (!methodName.equals("<init>")) {
//            foundSuperCall = true;
//            newStatements.add(logStatement);
//            newStatements.addAll(parameterProbes);
//
//            for (JCTree.JCStatement statement : blockStatements) {
////                currentLineNumber = getPositionToLine(classNode, statement.getStartPosition());
////                System.out.println("===>\t\tStatement type: " + statement.getClass());
////                ListBuffer<JCTree.JCStatement> normalizedStatements = normalizeStatements(statement, classNode);
//                newStatements.add(statement);
//            }
//
//
//        } else {
//            for (JCTree.JCStatement statement : blockStatements) {
//
//                System.out.println("===>\t\tStatement type: " + statement.getClass());
//                newStatements.add(statement);
//
//
//                if (!foundSuperCall) {
//                    if (statement instanceof JCTree.JCExpressionStatement) {
//                        JCTree.JCExpressionStatement expressionStatement = (JCTree.JCExpressionStatement) statement;
//                        JCTree.JCExpression expression = expressionStatement.getExpression();
//                        if (expression instanceof JCTree.JCMethodInvocation) {
//                            JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) expression;
//                            JCTree.JCExpression methodSelect = methodInvocation.getMethodSelect();
//                            if (methodSelect instanceof JCTree.JCIdent) {
//                                JCTree.JCIdent methodNameIdentifier = (JCTree.JCIdent) methodSelect;
//                                if ("super".equals(methodNameIdentifier.getName().toString())) {
//                                    foundSuperCall = true;
//                                    newStatements.add(logStatement);
//                                    newStatements.addAll(parameterProbes);
//                                }
//                            }
//                        }
//                    }
//                }
//
//
//            }
//            if (!foundSuperCall) {
//                newStatements.add(logStatement);
//            }
//        }
//
//
//        jcMethodDecl.body = treeMaker.Block(0, newStatements.toList());
//        System.out.println("After: " + jcMethodDecl.body);

    }

    private int getPositionToLine(JavacNode classNode, int startPosition) {
        String codeString = classNode.get().getTree().toString();
        if (startPosition > codeString.length()) {
            return 0;
        }
        codeString = codeString.substring(0, startPosition);
        return codeString.split("\\n").length;
    }

    private MethodCallInformation getMethodInformation(JCTree.JCExpression methodSelect) {
//        assert methodSelect instanceof JCTree.JCFieldAccess;
        if (methodSelect instanceof JCTree.JCFieldAccess) {
            JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) methodSelect;
            String subject = fieldAccess.getExpression().toString();
            String methodName = fieldAccess.getIdentifier().toString();
            return new MethodCallInformation(subject, methodName);
        }
//        assert false;
        return null;
    }

    private JCTree.JCExpressionStatement createLogStatement(JavacNode methodNode, JCTree.JCExpression ident, int dataId) {
        JavacTreeMaker treeMaker = methodNode.getTreeMaker();
        JCTree.JCExpression printlnMethod = JavacHandlerUtil.chainDotsString(methodNode,
                "io.unlogged.logging.Logging.recordEvent");

        return treeMaker.Exec(
                treeMaker.Apply(
                        List.<JCTree.JCExpression>nil(),
                        printlnMethod,
                        List.<JCTree.JCExpression>of(
                                ident,
                                treeMaker.Literal(dataId)
                        )
                )
        );
    }

    private String createMethodDescriptor(JavacNode methodNode) {
        return "(IL)Ljava.lang.Integer;";
    }

    public Map<JCTree.JCClassDecl, ClassInfo> getClassRoots() {
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
