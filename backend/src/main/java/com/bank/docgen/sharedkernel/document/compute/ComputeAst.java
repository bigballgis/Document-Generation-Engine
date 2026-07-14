package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;
import java.util.List;

final class ComputeAst {

    private ComputeAst() {
    }

    sealed interface Expr permits LiteralExpr, VariableRefExpr, FunctionCallExpr {
    }

    record LiteralExpr(Object value) implements Expr {
    }

    record VariableRefExpr(String path) implements Expr {
    }

    record FunctionCallExpr(String name, List<Expr> args) implements Expr {
    }

    static LiteralExpr number(BigDecimal value) {
        return new LiteralExpr(value);
    }

    static LiteralExpr string(String value) {
        return new LiteralExpr(value);
    }

    static LiteralExpr bool(boolean value) {
        return new LiteralExpr(value);
    }

    static LiteralExpr nullLiteral() {
        return new LiteralExpr(null);
    }
}
