package com.bank.docgen.sharedkernel.document.expression;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class ConditionExpressionParser {

    private static final String[] COMPARISON_OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};

    private final String input;
    private int pos;

    ConditionExpressionParser(String input) {
        this.input = input;
    }

    int position() {
        return pos;
    }

    void parseExpression() {
        parseOrExpr();
        skipWhitespace();
    }

    ConditionExpressionAst.Expr parseExpressionTree() {
        return parseOrExpr();
    }

    boolean isAtEnd() {
        skipWhitespace();
        return pos >= input.length();
    }

    private ConditionExpressionAst.Expr parseOrExpr() {
        List<ConditionExpressionAst.Expr> operands = new ArrayList<>();
        operands.add(parseAndExpr());
        while (tryConsume("||")) {
            operands.add(parseAndExpr());
        }
        return operands.size() == 1 ? operands.getFirst() : new ConditionExpressionAst.OrExpr(List.copyOf(operands));
    }

    private ConditionExpressionAst.Expr parseAndExpr() {
        List<ConditionExpressionAst.Expr> operands = new ArrayList<>();
        operands.add(parseUnaryExpr());
        while (tryConsume("&&")) {
            operands.add(parseUnaryExpr());
        }
        return operands.size() == 1 ? operands.getFirst() : new ConditionExpressionAst.AndExpr(List.copyOf(operands));
    }

    private ConditionExpressionAst.Expr parseUnaryExpr() {
        skipWhitespace();
        if (tryConsume("!")) {
            skipWhitespace();
            if (startsVariableReference()) {
                return new ConditionExpressionAst.NotExpr(new ConditionExpressionAst.BooleanVariableExpr(parseVariableReference()));
            }
            return new ConditionExpressionAst.NotExpr(parseUnaryExpr());
        }
        return parsePrimary();
    }

    private boolean startsVariableReference() {
        skipWhitespace();
        return pos + 2 <= input.length() && input.startsWith("${", pos);
    }

    private ConditionExpressionAst.Expr parsePrimary() {
        skipWhitespace();
        if (tryConsume("(")) {
            ConditionExpressionAst.Expr inner = parseOrExpr();
            expect(")");
            return inner;
        }
        if (startsVariableReference()) {
            int savedPosition = pos;
            String variableName = parseVariableReference();
            skipWhitespace();
            if (hasComparisonOperatorAhead()) {
                pos = savedPosition;
                return parseComparison();
            }
            return new ConditionExpressionAst.BooleanVariableExpr(variableName);
        }
        return parseComparison();
    }

    private boolean hasComparisonOperatorAhead() {
        skipWhitespace();
        for (String operator : COMPARISON_OPERATORS) {
            if (input.regionMatches(pos, operator, 0, operator.length())) {
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
        skipWhitespace();
        expect("${");
        int start = pos;
        if (pos >= input.length() || !Character.isLetter(input.charAt(pos))) {
            throw error("Expected variable name after '${'");
        }
        pos++;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '-')) {
                break;
            }
            pos++;
        }
        String variableName = input.substring(start, pos);
        expect("}");
        return variableName;
    }

    private String parseComparisonOperator() {
        skipWhitespace();
        for (String operator : COMPARISON_OPERATORS) {
            if (tryConsume(operator)) {
                return operator;
            }
        }
        throw error("Expected comparison operator");
    }

    private ConditionExpressionAst.RhsLiteral parseRhsLiteral() {
        skipWhitespace();
        if (tryConsume("null")) {
            return ConditionExpressionAst.RhsLiteral.nullLiteral();
        }
        if (tryConsume("true")) {
            return ConditionExpressionAst.RhsLiteral.booleanLiteral(true);
        }
        if (tryConsume("false")) {
            return ConditionExpressionAst.RhsLiteral.booleanLiteral(false);
        }
        if (pos < input.length() && input.charAt(pos) == '\'') {
            return ConditionExpressionAst.RhsLiteral.stringLiteral(parseQuotedString());
        }
        return ConditionExpressionAst.RhsLiteral.numberLiteral(parseNumber());
    }

    private String parseQuotedString() {
        expect("'");
        StringBuilder builder = new StringBuilder();
        String result = null;
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == '\'') {
                if (pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
                    builder.append('\'');
                    pos += 2;
                    continue;
                }
                pos++;
                result = builder.toString();
                break;
            }
            builder.append(ch);
            pos++;
        }
        if (result != null) {
            return result;
        }
        throw error("Unterminated string literal");
    }

    private BigDecimal parseNumber() {
        skipWhitespace();
        int start = pos;
        if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
            throw error("Expected number or string literal");
        }
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
        }
        String numberText = input.substring(start, pos);
        try {
            return new BigDecimal(numberText);
        } catch (NumberFormatException ex) {
            throw error("Invalid number literal");
        }
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private boolean tryConsume(String token) {
        skipWhitespace();
        if (input.regionMatches(pos, token, 0, token.length())) {
            if (token.chars().allMatch(Character::isLetter)) {
                int end = pos + token.length();
                if (end < input.length()) {
                    char next = input.charAt(end);
                    if (Character.isLetterOrDigit(next) || next == '_') {
                        return false;
                    }
                }
            }
            pos += token.length();
            return true;
        }
        return false;
    }

    private void expect(String token) {
        if (!tryConsume(token)) {
            throw error("Expected '" + token + "'");
        }
    }

    private ParseException error(String message) {
        return new ParseException(message + " at position " + pos);
    }

    static final class ParseException extends RuntimeException {

        ParseException(String message) {
            super(message);
        }
    }
}
