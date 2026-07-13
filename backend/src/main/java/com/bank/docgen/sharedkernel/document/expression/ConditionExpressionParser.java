package com.bank.docgen.sharedkernel.document.expression;

import java.util.ArrayList;
import java.util.List;

final class ConditionExpressionParser {

    private static final String[] COMPARISON_OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};

    private final ConditionExpressionLexSupport lex;

    ConditionExpressionParser(String input) {
        this.lex = new ConditionExpressionLexSupport(input);
    }

    int position() {
        return lex.position();
    }

    void parseExpression() {
        parseOrExpr();
        lex.skipWhitespace();
    }

    ConditionExpressionAst.Expr parseExpressionTree() {
        return parseOrExpr();
    }

    boolean isAtEnd() {
        lex.skipWhitespace();
        return lex.position() >= lex.input().length();
    }

    private ConditionExpressionAst.Expr parseOrExpr() {
        List<ConditionExpressionAst.Expr> operands = new ArrayList<>();
        operands.add(parseAndExpr());
        while (lex.tryConsume("||")) {
            operands.add(parseAndExpr());
        }
        return operands.size() == 1 ? operands.getFirst() : new ConditionExpressionAst.OrExpr(List.copyOf(operands));
    }

    private ConditionExpressionAst.Expr parseAndExpr() {
        List<ConditionExpressionAst.Expr> operands = new ArrayList<>();
        operands.add(parseUnaryExpr());
        while (lex.tryConsume("&&")) {
            operands.add(parseUnaryExpr());
        }
        return operands.size() == 1 ? operands.getFirst() : new ConditionExpressionAst.AndExpr(List.copyOf(operands));
    }

    private ConditionExpressionAst.Expr parseUnaryExpr() {
        lex.skipWhitespace();
        if (lex.tryConsume("!")) {
            lex.skipWhitespace();
            if (startsVariableReference()) {
                return new ConditionExpressionAst.NotExpr(new ConditionExpressionAst.BooleanVariableExpr(parseVariableReference()));
            }
            return new ConditionExpressionAst.NotExpr(parseUnaryExpr());
        }
        return parsePrimary();
    }

    private boolean startsVariableReference() {
        lex.skipWhitespace();
        return lex.position() + 2 <= lex.input().length() && lex.input().startsWith("${", lex.position());
    }

    private ConditionExpressionAst.Expr parsePrimary() {
        lex.skipWhitespace();
        if (lex.tryConsume("(")) {
            ConditionExpressionAst.Expr inner = parseOrExpr();
            lex.expect(")");
            return inner;
        }
        if (startsVariableReference()) {
            int savedPosition = lex.position();
            String variableName = parseVariableReference();
            lex.skipWhitespace();
            if (hasComparisonOperatorAhead()) {
                lex.setPosition(savedPosition);
                return parseComparison();
            }
            return new ConditionExpressionAst.BooleanVariableExpr(variableName);
        }
        return parseComparison();
    }

    private boolean hasComparisonOperatorAhead() {
        lex.skipWhitespace();
        for (String operator : COMPARISON_OPERATORS) {
            if (lex.input().regionMatches(lex.position(), operator, 0, operator.length())) {
                return true;
            }
        }
        return false;
    }

    private ConditionExpressionAst.ComparisonExpr parseComparison() {
        String variableName = parseVariableReference();
        String operator = parseComparisonOperator();
        ConditionExpressionAst.RhsLiteral rhs = parseRhsLiteral();
        return new ConditionExpressionAst.ComparisonExpr(variableName, operator, rhs);
    }

    private String parseVariableReference() {
        lex.skipWhitespace();
        lex.expect("${");
        int start = lex.position();
        String input = lex.input();
        int pos = start;
        if (pos >= input.length() || !Character.isLetter(input.charAt(pos))) {
            throw lex.error("Expected variable name after '${'");
        }
        pos++;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '-')) {
                break;
            }
            pos++;
        }
        lex.setPosition(pos);
        String variableName = input.substring(start, pos);
        lex.expect("}");
        return variableName;
    }

    private String parseComparisonOperator() {
        lex.skipWhitespace();
        for (String operator : COMPARISON_OPERATORS) {
            if (lex.tryConsume(operator)) {
                return operator;
            }
        }
        throw lex.error("Expected comparison operator");
    }

    private ConditionExpressionAst.RhsLiteral parseRhsLiteral() {
        lex.skipWhitespace();
        if (lex.tryConsume("null")) {
            return ConditionExpressionAst.RhsLiteral.nullLiteral();
        }
        if (lex.tryConsume("true")) {
            return ConditionExpressionAst.RhsLiteral.booleanLiteral(true);
        }
        if (lex.tryConsume("false")) {
            return ConditionExpressionAst.RhsLiteral.booleanLiteral(false);
        }
        if (lex.position() < lex.input().length() && lex.input().charAt(lex.position()) == '\'') {
            return ConditionExpressionAst.RhsLiteral.stringLiteral(lex.parseQuotedString());
        }
        return ConditionExpressionAst.RhsLiteral.numberLiteral(lex.parseNumber());
    }

    static final class ParseException extends RuntimeException {

        ParseException(String message) {
            super(message);
        }
    }
}
