package com.bank.docgen.sharedkernel.document.expression;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Safe subset interpreter for structured-content and template-rule condition expressions (F3-C2 / F3-C7).
 */
public final class ConditionExpressionEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(ConditionExpressionEvaluator.class);

    public static final ConditionExpressionEvaluator INSTANCE = new ConditionExpressionEvaluator();

    private ConditionExpressionEvaluator() {
    }

    public List<String> validateSyntax(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of("Expression is empty");
        }
        try {
            Parser parser = new Parser(expression.trim());
            parser.parseExpression();
            if (!parser.isAtEnd()) {
                return List.of("Unexpected trailing input at position " + parser.position());
            }
            return List.of();
        } catch (ParseException ex) {
            return List.of(ex.getMessage());
        }
    }

    public List<String> extractVariableReferences(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        if (!validateSyntax(expression).isEmpty()) {
            return List.of();
        }
        Parser parser = new Parser(expression.trim());
        Expr expr = parser.parseExpressionTree();
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        collectVariables(expr, refs);
        return List.copyOf(refs);
    }

    public boolean evaluate(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            LOG.debug("Condition expression is blank; treating as false");
            return false;
        }
        if (!validateSyntax(expression).isEmpty()) {
            LOG.debug("Condition expression failed syntax validation; treating as false");
            return false;
        }
        try {
            Parser parser = new Parser(expression.trim());
            Expr expr = parser.parseExpressionTree();
            return evaluateExpr(expr, variables == null ? Map.of() : variables);
        } catch (ParseException ex) {
            LOG.debug("Condition expression evaluation failed; treating as false");
            return false;
        }
    }

    private static void collectVariables(Expr expr, LinkedHashSet<String> refs) {
        switch (expr) {
            case ComparisonExpr comparison -> refs.add(comparison.variableName());
            case BooleanVariableExpr booleanVariable -> refs.add(booleanVariable.variableName());
            case NotExpr notExpr -> collectVariables(notExpr.operand(), refs);
            case OrExpr orExpr -> orExpr.operands().forEach(operand -> collectVariables(operand, refs));
            case AndExpr andExpr -> andExpr.operands().forEach(operand -> collectVariables(operand, refs));
            default -> { }
        }
    }

    private static boolean evaluateExpr(Expr expr, Map<String, Object> variables) {
        return switch (expr) {
            case ComparisonExpr comparison -> evaluateComparison(comparison, variables);
            case BooleanVariableExpr booleanVariable -> toBoolean(variables.get(booleanVariable.variableName()));
            case NotExpr notExpr -> !evaluateExpr(notExpr.operand(), variables);
            case OrExpr orExpr -> {
                for (Expr operand : orExpr.operands()) {
                    if (evaluateExpr(operand, variables)) {
                        yield true;
                    }
                }
                yield false;
            }
            case AndExpr andExpr -> {
                for (Expr operand : andExpr.operands()) {
                    if (!evaluateExpr(operand, variables)) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    private static boolean evaluateComparison(ComparisonExpr comparison, Map<String, Object> variables) {
        Object left = variables.get(comparison.variableName());
        RhsLiteral rhs = comparison.rhs();
        String op = comparison.operator();

        if (rhs.kind() == RhsKind.NULL) {
            return switch (op) {
                case "==" -> left == null;
                case "!=" -> left != null;
                default -> false;
            };
        }
        if (rhs.kind() == RhsKind.BOOLEAN) {
            boolean leftBool = left == null ? false : Boolean.parseBoolean(String.valueOf(left));
            boolean rightBool = rhs.booleanValue();
            return switch (op) {
                case "==" -> leftBool == rightBool;
                case "!=" -> leftBool != rightBool;
                default -> false;
            };
        }

        Optional<BigDecimal> leftNumber = toBigDecimal(left);
        Optional<BigDecimal> rightNumber = rhs.numericValue();
        if (leftNumber.isPresent() && rightNumber.isPresent()) {
            return compareNumbers(leftNumber.get(), rightNumber.get(), op);
        }

        String leftString = left == null ? "" : String.valueOf(left);
        String rightString = rhs.literalText();
        return compareStrings(leftString, rightString, op);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean compareNumbers(BigDecimal left, BigDecimal right, String op) {
        int cmp = left.compareTo(right);
        return switch (op) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private static boolean compareStrings(String left, String right, String op) {
        int cmp = left.compareTo(right);
        return switch (op) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case ">=" -> cmp >= 0;
            case "<" -> cmp < 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private static Optional<BigDecimal> toBigDecimal(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof BigDecimal decimal) {
            return Optional.of(decimal);
        }
        if (value instanceof Number number) {
            return Optional.of(new BigDecimal(number.toString()));
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(text));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private sealed interface Expr permits ComparisonExpr, BooleanVariableExpr, NotExpr, OrExpr, AndExpr {
    }

    private record ComparisonExpr(String variableName, String operator, RhsLiteral rhs) implements Expr {
    }

    private record BooleanVariableExpr(String variableName) implements Expr {
    }

    private record NotExpr(Expr operand) implements Expr {
    }

    private record OrExpr(List<Expr> operands) implements Expr {
    }

    private record AndExpr(List<Expr> operands) implements Expr {
    }

    private enum RhsKind {
        NULL,
        BOOLEAN,
        NUMBER,
        STRING
    }

    private record RhsLiteral(RhsKind kind, Boolean booleanValue, BigDecimal numberValue, String stringValue) {

        static RhsLiteral nullLiteral() {
            return new RhsLiteral(RhsKind.NULL, null, null, null);
        }

        static RhsLiteral booleanLiteral(boolean value) {
            return new RhsLiteral(RhsKind.BOOLEAN, value, null, null);
        }

        static RhsLiteral numberLiteral(BigDecimal value) {
            return new RhsLiteral(RhsKind.NUMBER, null, value, null);
        }

        static RhsLiteral stringLiteral(String value) {
            return new RhsLiteral(RhsKind.STRING, null, null, value);
        }

        Optional<BigDecimal> numericValue() {
            if (kind == RhsKind.NUMBER) {
                return Optional.ofNullable(numberValue);
            }
            if (kind == RhsKind.STRING && stringValue != null) {
                try {
                    return Optional.of(new BigDecimal(stringValue.trim()));
                } catch (NumberFormatException ex) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }

        String literalText() {
            if (kind == RhsKind.STRING) {
                return stringValue;
            }
            if (kind == RhsKind.NUMBER && numberValue != null) {
                return numberValue.toPlainString();
            }
            return "";
        }
    }

    private static final class Parser {

        private static final String[] COMPARISON_OPERATORS = {"==", "!=", ">=", "<=", ">", "<"};

        private final String input;
        private int pos;

        private Parser(String input) {
            this.input = input;
        }

        private int position() {
            return pos;
        }

        private void parseExpression() {
            parseOrExpr();
            skipWhitespace();
        }

        private Expr parseExpressionTree() {
            return parseOrExpr();
        }

        private boolean isAtEnd() {
            skipWhitespace();
            return pos >= input.length();
        }

        private Expr parseOrExpr() {
            List<Expr> operands = new ArrayList<>();
            operands.add(parseAndExpr());
            while (tryConsume("||")) {
                operands.add(parseAndExpr());
            }
            return operands.size() == 1 ? operands.getFirst() : new OrExpr(List.copyOf(operands));
        }

        private Expr parseAndExpr() {
            List<Expr> operands = new ArrayList<>();
            operands.add(parseUnaryExpr());
            while (tryConsume("&&")) {
                operands.add(parseUnaryExpr());
            }
            return operands.size() == 1 ? operands.getFirst() : new AndExpr(List.copyOf(operands));
        }

        private Expr parseUnaryExpr() {
            skipWhitespace();
            if (tryConsume("!")) {
                skipWhitespace();
                if (startsVariableReference()) {
                    return new NotExpr(new BooleanVariableExpr(parseVariableReference()));
                }
                return new NotExpr(parseUnaryExpr());
            }
            return parsePrimary();
        }

        private boolean startsVariableReference() {
            skipWhitespace();
            return pos + 2 <= input.length() && input.startsWith("${", pos);
        }

        private Expr parsePrimary() {
            skipWhitespace();
            if (tryConsume("(")) {
                Expr inner = parseOrExpr();
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
                return new BooleanVariableExpr(variableName);
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

        private ComparisonExpr parseComparison() {
            String variableName = parseVariableReference();
            String operator = parseComparisonOperator();
            RhsLiteral rhs = parseRhsLiteral();
            return new ComparisonExpr(variableName, operator, rhs);
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

        private RhsLiteral parseRhsLiteral() {
            skipWhitespace();
            if (tryConsume("null")) {
                return RhsLiteral.nullLiteral();
            }
            if (tryConsume("true")) {
                return RhsLiteral.booleanLiteral(true);
            }
            if (tryConsume("false")) {
                return RhsLiteral.booleanLiteral(false);
            }
            if (pos < input.length() && input.charAt(pos) == '\'') {
                return RhsLiteral.stringLiteral(parseQuotedString());
            }
            return RhsLiteral.numberLiteral(parseNumber());
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
    }

    private static final class ParseException extends RuntimeException {

        private ParseException(String message) {
            super(message);
        }
    }
}
