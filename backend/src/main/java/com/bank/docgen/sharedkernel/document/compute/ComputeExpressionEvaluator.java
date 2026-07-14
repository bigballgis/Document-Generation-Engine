package com.bank.docgen.sharedkernel.document.compute;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates a parsed compute AST against a binding context.
 */
final class ComputeExpressionEvaluator {

    private final Map<String, Object> bindings;
    private final Locale locale;
    private final String failingVariableKey;
    private final String expression;

    ComputeExpressionEvaluator(
            Map<String, Object> bindings,
            Locale locale,
            String failingVariableKey,
            String expression
    ) {
        this.bindings = bindings;
        this.locale = locale;
        this.failingVariableKey = failingVariableKey;
        this.expression = expression;
    }

    Object evaluate(ComputeAst.Expr expr) {
        return switch (expr) {
            case ComputeAst.LiteralExpr literal -> literal.value();
            case ComputeAst.VariableRefExpr ref -> resolvePath(ref.path());
            case ComputeAst.FunctionCallExpr call -> evaluateFunction(call);
        };
    }

    static Set<String> collectVariableRoots(ComputeAst.Expr expr) {
        LinkedHashSet<String> roots = new LinkedHashSet<>();
        collectRefs(expr, roots);
        return roots;
    }

    static Set<String> collectVariablePaths(ComputeAst.Expr expr) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        collectPaths(expr, paths);
        return paths;
    }

    private static void collectRefs(ComputeAst.Expr expr, Set<String> roots) {
        switch (expr) {
            case ComputeAst.VariableRefExpr ref -> roots.add(rootOf(ref.path()));
            case ComputeAst.FunctionCallExpr call -> call.args().forEach(arg -> collectRefs(arg, roots));
            case ComputeAst.LiteralExpr ignored -> { }
        }
    }

    private static void collectPaths(ComputeAst.Expr expr, Set<String> paths) {
        switch (expr) {
            case ComputeAst.VariableRefExpr ref -> paths.add(ref.path());
            case ComputeAst.FunctionCallExpr call -> call.args().forEach(arg -> collectPaths(arg, paths));
            case ComputeAst.LiteralExpr ignored -> { }
        }
    }

    private static String rootOf(String path) {
        int dot = path.indexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private Object evaluateFunction(ComputeAst.FunctionCallExpr call) {
        return switch (call.name()) {
            case "COALESCE" -> evalCoalesce(call.args());
            case "SUM" -> evalSum(requireOne(call));
            case "COUNT" -> evalCount(requireOne(call));
            case "AVG" -> evalAvg(requireOne(call));
            case "FILTER" -> evalFilter(call.args());
            case "FORMAT_AMOUNT" -> evalFormatAmount(requireOne(call));
            case "FORMAT_DATE" -> evalFormatDate(requireOne(call));
            case "SPELL_AMOUNT" -> evalSpellAmount(requireOne(call));
            default -> fail("Unknown function '" + call.name() + "'");
        };
    }

    private ComputeAst.Expr requireOne(ComputeAst.FunctionCallExpr call) {
        if (call.args().size() != 1) {
            fail(call.name() + " requires exactly 1 argument");
        }
        return call.args().getFirst();
    }

    private Object evalCoalesce(List<ComputeAst.Expr> args) {
        if (args.isEmpty()) {
            fail("COALESCE requires at least one argument");
        }
        for (ComputeAst.Expr arg : args) {
            Object value = evaluate(arg);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal evalSum(ComputeAst.Expr collectionExpr) {
        List<?> items = asCollection(evaluate(collectionExpr));
        BigDecimal sum = BigDecimal.ZERO;
        for (Object item : items) {
            sum = sum.add(toNumber(item, "SUM"));
        }
        return sum;
    }

    private long evalCount(ComputeAst.Expr collectionExpr) {
        return asCollection(evaluate(collectionExpr)).size();
    }

    private BigDecimal evalAvg(ComputeAst.Expr collectionExpr) {
        List<?> items = asCollection(evaluate(collectionExpr));
        if (items.isEmpty()) {
            fail("AVG of empty collection is undefined");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Object item : items) {
            sum = sum.add(toNumber(item, "AVG"));
        }
        return sum.divide(BigDecimal.valueOf(items.size()), 10, RoundingMode.HALF_UP);
    }

    private List<Object> evalFilter(List<ComputeAst.Expr> args) {
        if (args.size() < 3 || args.size() > 4) {
            fail("FILTER requires collection, fieldPath, op, and optional literal");
        }
        List<?> items = asCollection(evaluate(args.get(0)));
        String fieldPath = stringArg(args.get(1), "FILTER fieldPath");
        String op = stringArg(args.get(2), "FILTER op");
        Object literal = args.size() == 4 ? evaluate(args.get(3)) : null;
        Set<String> allowedOps = Set.of("EQ", "NE", "GT", "GE", "LT", "LE", "IS_NULL", "IS_NOT_NULL");
        if (!allowedOps.contains(op)) {
            fail("FILTER op '" + op + "' is not allowed");
        }
        // Project matching field values so SUM/AVG/COUNT compose naturally
        // (BDD-CE-K03-009: SUM(FILTER(${items}, amount, GT, 0))).
        List<Object> result = new ArrayList<>();
        for (Object item : items) {
            Object fieldValue = readRelative(item, fieldPath);
            if (matchesFilter(fieldValue, op, literal)) {
                result.add(fieldValue);
            }
        }
        return result;
    }

    private boolean matchesFilter(Object fieldValue, String op, Object literal) {
        return switch (op) {
            case "IS_NULL" -> fieldValue == null;
            case "IS_NOT_NULL" -> fieldValue != null;
            case "EQ" -> compareValues(fieldValue, literal) == 0;
            case "NE" -> compareValues(fieldValue, literal) != 0;
            case "GT" -> compareValues(fieldValue, literal) > 0;
            case "GE" -> compareValues(fieldValue, literal) >= 0;
            case "LT" -> compareValues(fieldValue, literal) < 0;
            case "LE" -> compareValues(fieldValue, literal) <= 0;
            default -> fail("FILTER op '" + op + "' is not allowed");
        };
    }

    private int compareValues(Object left, Object right) {
        if (left == null || right == null) {
            fail("Cannot compare null in FILTER relational op");
        }
        if (left instanceof Number || right instanceof Number || left instanceof BigDecimal || right instanceof BigDecimal) {
            return toNumber(left, "FILTER").compareTo(toNumber(right, "FILTER"));
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private String evalFormatAmount(ComputeAst.Expr valueExpr) {
        BigDecimal amount = toNumber(evaluate(valueExpr), "FORMAT_AMOUNT");
        // Deterministic currency-style formatting so golden/unit expectations stay stable across JDKs.
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }

    private String evalFormatDate(ComputeAst.Expr valueExpr) {
        Object value = evaluate(valueExpr);
        if (value == null) {
            fail("FORMAT_DATE value is null");
        }
        LocalDate date = toLocalDate(value);
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
        return formatter.format(date);
    }

    private String evalSpellAmount(ComputeAst.Expr valueExpr) {
        Object value = evaluate(valueExpr);
        if (value == null) {
            fail("SPELL_AMOUNT value is null");
        }
        try {
            return SpellAmountCn.spell(toNumber(value, "SPELL_AMOUNT"));
        } catch (IllegalArgumentException | ArithmeticException ex) {
            fail("SPELL_AMOUNT rejected: " + ex.getMessage());
            return null;
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        String text = String.valueOf(value).trim();
        if (text.length() >= 10 && text.charAt(4) == '-') {
            return LocalDate.parse(text.substring(0, 10));
        }
        fail("FORMAT_DATE expected ISO date");
        return null;
    }

    private Object resolvePath(String path) {
        String[] segments = path.split("\\.");
        // Missing root is treated as null so COALESCE can skip it; functions that require a
        // value still fail-closed when they receive null (K03-C6 / BDD-CE-K03-006 vs 021).
        if (!bindings.containsKey(segments[0])) {
            return null;
        }
        Object current = bindings.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            if (current == null) {
                return null;
            }
            current = readRelative(current, segments[i]);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Object readRelative(Object source, String path) {
        if (source == null) {
            return null;
        }
        String[] segments = path.split("\\.");
        Object current = source;
        for (String segment : segments) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(segment);
            } else {
                fail("Cannot resolve field '" + path + "'");
            }
        }
        return current;
    }

    private List<?> asCollection(Object value) {
        if (value == null) {
            fail("Expected collection, got null");
        }
        if (value instanceof List<?> list) {
            if (list.size() > ComputeDslLimits.MAX_COLLECTION_SIZE) {
                fail("Collection exceeds max size");
            }
            return list;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.size() > ComputeDslLimits.MAX_COLLECTION_SIZE) {
                fail("Collection exceeds max size");
            }
            return List.copyOf(collection);
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            if (length > ComputeDslLimits.MAX_COLLECTION_SIZE) {
                fail("Collection exceeds max size");
            }
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(value, i));
            }
            return list;
        }
        fail("Expected collection");
        return List.of();
    }

    private BigDecimal toNumber(Object value, String function) {
        if (value == null) {
            fail(function + " expected number, got null");
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            fail(function + " expected number");
            return null;
        }
    }

    private String stringArg(ComputeAst.Expr expr, String label) {
        Object value = evaluate(expr);
        if (value == null) {
            fail(label + " is null");
        }
        return String.valueOf(value);
    }

    private <T> T fail(String reason) {
        throw new VariableComputeException(failingVariableKey, expression, reason);
    }
}
