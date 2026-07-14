package com.bank.docgen.sharedkernel.document.compute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser for the whitelist compute DSL (CE-K03).
 */
final class ComputeExpressionParser {

    private static final Set<String> WHITELIST = Set.of(
            "COALESCE",
            "SUM",
            "COUNT",
            "AVG",
            "FILTER",
            "FORMAT_AMOUNT",
            "FORMAT_DATE",
            "SPELL_AMOUNT"
    );

    private final ComputeExpressionLexSupport lex;
    private int maxDepthObserved;

    ComputeExpressionParser(String input) {
        this.lex = new ComputeExpressionLexSupport(input);
    }

    int maxDepthObserved() {
        return maxDepthObserved;
    }

    ComputeAst.Expr parseExpressionTree() {
        ComputeAst.Expr expr = parseExpr(0);
        lex.skipWhitespace();
        if (lex.position() < lex.input().length()) {
            throw lex.error("Unexpected trailing input");
        }
        return expr;
    }

    private ComputeAst.Expr parseExpr(int depth) {
        lex.skipWhitespace();
        if (lex.position() + 2 <= lex.input().length()
                && lex.input().startsWith("${", lex.position())) {
            return new ComputeAst.VariableRefExpr(lex.parseVariablePath());
        }
        if (lex.tryConsume("null")) {
            return ComputeAst.nullLiteral();
        }
        if (lex.tryConsume("true")) {
            return ComputeAst.bool(true);
        }
        if (lex.tryConsume("false")) {
            return ComputeAst.bool(false);
        }
        lex.skipWhitespace();
        if (lex.position() < lex.input().length()) {
            char ch = lex.input().charAt(lex.position());
            if (ch == '\'' || ch == '"') {
                return ComputeAst.string(lex.parseQuotedString());
            }
            if (Character.isDigit(ch) || ch == '+' || ch == '-') {
                return ComputeAst.number(lex.parseNumber());
            }
        }
        if (lex.startsIdentifier()) {
            String name = lex.parseIdentifier();
            lex.skipWhitespace();
            if (lex.position() < lex.input().length() && lex.input().charAt(lex.position()) == '(') {
                return parseFunctionCall(name, depth);
            }
            // Bare identifier only valid as FILTER fieldPath / op when nested; top-level reject.
            throw lex.error("Unexpected identifier '" + name + "'");
        }
        throw lex.error("Expected expression");
    }

    private ComputeAst.Expr parseFunctionCall(String name, int depth) {
        int nextDepth = depth + 1;
        if (nextDepth > ComputeDslLimits.MAX_NESTING_DEPTH) {
            throw lex.error("Function nesting depth exceeds limit");
        }
        maxDepthObserved = Math.max(maxDepthObserved, nextDepth);
        if (!WHITELIST.contains(name)) {
            throw lex.error("Unknown function '" + name + "'");
        }
        lex.expect("(");
        List<ComputeAst.Expr> args = new ArrayList<>();
        lex.skipWhitespace();
        if (!(lex.position() < lex.input().length() && lex.input().charAt(lex.position()) == ')')) {
            if ("FILTER".equals(name)) {
                args.add(parseExpr(nextDepth));
                lex.expect(",");
                args.add(ComputeAst.string(lex.parseFieldPath()));
                lex.expect(",");
                String op = lex.parseIdentifier();
                args.add(ComputeAst.string(op));
                lex.skipWhitespace();
                if (lex.position() < lex.input().length() && lex.input().charAt(lex.position()) == ',') {
                    lex.expect(",");
                    args.add(parseFilterLiteral(nextDepth));
                } else {
                    args.add(ComputeAst.nullLiteral());
                }
            } else {
                args.add(parseExpr(nextDepth));
                while (lex.tryConsume(",")) {
                    args.add(parseExpr(nextDepth));
                }
            }
        }
        lex.expect(")");
        return new ComputeAst.FunctionCallExpr(name, List.copyOf(args));
    }

    private ComputeAst.Expr parseFilterLiteral(int depth) {
        lex.skipWhitespace();
        if (lex.tryConsume("null")) {
            return ComputeAst.nullLiteral();
        }
        if (lex.tryConsume("true")) {
            return ComputeAst.bool(true);
        }
        if (lex.tryConsume("false")) {
            return ComputeAst.bool(false);
        }
        if (lex.position() < lex.input().length()) {
            char ch = lex.input().charAt(lex.position());
            if (ch == '\'' || ch == '"') {
                return ComputeAst.string(lex.parseQuotedString());
            }
            if (Character.isDigit(ch) || ch == '+' || ch == '-') {
                return ComputeAst.number(lex.parseNumber());
            }
        }
        return parseExpr(depth);
    }

    static Set<String> whitelist() {
        return WHITELIST;
    }
}
