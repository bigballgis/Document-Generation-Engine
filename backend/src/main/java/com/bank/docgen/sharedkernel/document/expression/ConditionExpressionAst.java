package com.bank.docgen.sharedkernel.document.expression;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Package-private AST nodes for {@link ConditionExpressionEvaluator}. */
interface ConditionExpressionAst {

    sealed interface Expr permits ComparisonExpr, BooleanVariableExpr, NotExpr, OrExpr, AndExpr {
    }

    record ComparisonExpr(String variableName, String operator, RhsLiteral rhs) implements Expr {
    }

    record BooleanVariableExpr(String variableName) implements Expr {
    }

    record NotExpr(Expr operand) implements Expr {
    }

    record OrExpr(List<Expr> operands) implements Expr {
    }

    record AndExpr(List<Expr> operands) implements Expr {
    }

    enum RhsKind {
        NULL,
        BOOLEAN,
        NUMBER,
        STRING
    }

    record RhsLiteral(RhsKind kind, Boolean booleanValue, BigDecimal numberValue, String stringValue) {

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
}
