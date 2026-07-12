package com.bank.docgen.sharedkernel.document.expression;

import java.math.BigDecimal;
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
            ConditionExpressionParser parser = new ConditionExpressionParser(expression.trim());
            parser.parseExpression();
            if (!parser.isAtEnd()) {
                return List.of("Unexpected trailing input at position " + parser.position());
            }
            return List.of();
        } catch (ConditionExpressionParser.ParseException ex) {
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
        ConditionExpressionParser parser = new ConditionExpressionParser(expression.trim());
        ConditionExpressionAst.Expr expr = parser.parseExpressionTree();
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
            ConditionExpressionParser parser = new ConditionExpressionParser(expression.trim());
            ConditionExpressionAst.Expr expr = parser.parseExpressionTree();
            return evaluateExpr(expr, variables == null ? Map.of() : variables);
        } catch (ConditionExpressionParser.ParseException ex) {
            LOG.debug("Condition expression evaluation failed; treating as false");
            return false;
        }
    }

    private static void collectVariables(ConditionExpressionAst.Expr expr, LinkedHashSet<String> refs) {
        switch (expr) {
            case ConditionExpressionAst.ComparisonExpr comparison -> refs.add(comparison.variableName());
            case ConditionExpressionAst.BooleanVariableExpr booleanVariable -> refs.add(booleanVariable.variableName());
            case ConditionExpressionAst.NotExpr notExpr -> collectVariables(notExpr.operand(), refs);
            case ConditionExpressionAst.OrExpr orExpr -> orExpr.operands().forEach(operand -> collectVariables(operand, refs));
            case ConditionExpressionAst.AndExpr andExpr -> andExpr.operands().forEach(operand -> collectVariables(operand, refs));
            default -> { }
        }
    }

    private static boolean evaluateExpr(ConditionExpressionAst.Expr expr, Map<String, Object> variables) {
        return switch (expr) {
            case ConditionExpressionAst.ComparisonExpr comparison -> evaluateComparison(comparison, variables);
            case ConditionExpressionAst.BooleanVariableExpr booleanVariable -> toBoolean(variables.get(booleanVariable.variableName()));
            case ConditionExpressionAst.NotExpr notExpr -> !evaluateExpr(notExpr.operand(), variables);
            case ConditionExpressionAst.OrExpr orExpr -> {
                for (ConditionExpressionAst.Expr operand : orExpr.operands()) {
                    if (evaluateExpr(operand, variables)) {
                        yield true;
                    }
                }
                yield false;
            }
            case ConditionExpressionAst.AndExpr andExpr -> {
                for (ConditionExpressionAst.Expr operand : andExpr.operands()) {
                    if (!evaluateExpr(operand, variables)) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    private static boolean evaluateComparison(ConditionExpressionAst.ComparisonExpr comparison, Map<String, Object> variables) {
        Object left = variables.get(comparison.variableName());
        ConditionExpressionAst.RhsLiteral rhs = comparison.rhs();
        String op = comparison.operator();

        if (rhs.kind() == ConditionExpressionAst.RhsKind.NULL) {
            return switch (op) {
                case "==" -> left == null;
                case "!=" -> left != null;
                default -> false;
            };
        }
        if (rhs.kind() == ConditionExpressionAst.RhsKind.BOOLEAN) {
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
}
