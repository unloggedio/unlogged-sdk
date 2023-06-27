package io.unlogged.weaver;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.ListBuffer;

public class NormalizedStatement {
    ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
    String resultVariableName;

    public JCTree.JCExpression getRemainderExpression() {
        return remainderExpression;
    }

    private JCTree.JCExpression remainderExpression;

    public ListBuffer<JCTree.JCStatement> getStatements() {
        return statements;
    }

    public void addStatement(JCTree.JCStatement statement) {
        statements.add(statement);
    }

    public void addAllStatement(ListBuffer<JCTree.JCStatement> statement) {
        statements.addAll(statement);
    }

    public void setStatements(ListBuffer<JCTree.JCStatement> statements) {
        this.statements = statements;
    }

    public String getResultVariableName() {
        return resultVariableName;
    }

    public void setResultVariableName(String resultVariableName) {
        this.resultVariableName = resultVariableName;
    }

    public void setRemainderExpression(JCTree.JCExpression expression) {
        this.remainderExpression = expression;
    }
}
