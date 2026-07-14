package com.bank.docgen.sharedkernel.document.compute;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Whitelist DSL compute engine — evaluate template compute variables before DOCX assembly (CE-K03).
 *
 * <p>Caller-supplied values for compute keys are ignored and overwritten by engine results (K03-C4).
 */
public final class VariableComputeEngine {

    public static final VariableComputeEngine INSTANCE = new VariableComputeEngine();

    private VariableComputeEngine() {
    }

    /**
     * Validate syntax, nesting/length bounds, and that every {@code ${path}} root exists in
     * {@code knownVariableKeys}.
     */
    public void validateExpression(
            String variableKey,
            String expression,
            Set<String> knownVariableKeys
    ) {
        validateSyntaxAndBounds(variableKey, expression);
        ComputeAst.Expr ast;
        try {
            ComputeExpressionParser parser = new ComputeExpressionParser(expression.trim());
            ast = parser.parseExpressionTree();
        } catch (ComputeExpressionLexSupport.ParseException ex) {
            throw new VariableComputeException(variableKey, expression, ex.getMessage());
        }
        Set<String> roots = ComputeExpressionEvaluator.collectVariableRoots(ast);
        for (String root : roots) {
            if (knownVariableKeys == null || !knownVariableKeys.contains(root)) {
                throw new VariableComputeException(
                        variableKey,
                        expression,
                        "Referenced variable '" + root + "' does not exist"
                );
            }
        }
    }

    /**
     * Evaluate a single expression against sample/bindings (author sample preview).
     * Syntax/length/depth are enforced; reference existence is the caller's responsibility
     * via {@link #validateExpression}.
     */
    public Object evaluateSingle(
            String variableKey,
            String expression,
            Map<String, Object> bindings,
            String localeTag
    ) {
        validateSyntaxAndBounds(variableKey, expression);
        try {
            ComputeExpressionParser parser = new ComputeExpressionParser(expression.trim());
            ComputeAst.Expr ast = parser.parseExpressionTree();
            Locale locale = resolveLocale(localeTag);
            return new ComputeExpressionEvaluator(
                    bindings == null ? Map.of() : bindings,
                    locale,
                    variableKey,
                    expression
            ).evaluate(ast);
        } catch (ComputeExpressionLexSupport.ParseException ex) {
            throw new VariableComputeException(variableKey, expression, ex.getMessage());
        }
    }

    public void validateSyntaxAndBounds(String variableKey, String expression) {
        if (expression == null || expression.isBlank()) {
            throw new VariableComputeException(variableKey, expression, "computeExpression is blank");
        }
        if (expression.length() > ComputeDslLimits.MAX_EXPRESSION_LENGTH) {
            throw new VariableComputeException(variableKey, expression, "computeExpression exceeds max length");
        }
        try {
            ComputeExpressionParser parser = new ComputeExpressionParser(expression.trim());
            parser.parseExpressionTree();
            if (parser.maxDepthObserved() > ComputeDslLimits.MAX_NESTING_DEPTH) {
                throw new VariableComputeException(variableKey, expression, "nesting depth exceeds limit");
            }
        } catch (ComputeExpressionLexSupport.ParseException ex) {
            throw new VariableComputeException(variableKey, expression, ex.getMessage());
        }
    }

    /**
     * Apply all compute variables onto a copy of {@code inputVariables}. Compute keys supplied by
     * the caller are stripped then overwritten with engine results.
     */
    public Map<String, Object> evaluateAll(
            List<ComputeVariableDefinition> definitions,
            Map<String, Object> inputVariables,
            String localeTag
    ) {
        List<ComputeVariableDefinition> computeVars = definitions == null
                ? List.of()
                : definitions.stream().filter(ComputeVariableDefinition::isCompute).toList();
        Map<String, Object> bindings = new LinkedHashMap<>();
        if (inputVariables != null) {
            bindings.putAll(inputVariables);
        }
        Set<String> computeKeys = new HashSet<>();
        for (ComputeVariableDefinition definition : computeVars) {
            computeKeys.add(definition.variableKey());
        }
        for (String computeKey : computeKeys) {
            bindings.remove(computeKey);
        }
        if (computeVars.isEmpty()) {
            return bindings;
        }

        Map<String, String> expressions = new LinkedHashMap<>();
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        Set<String> knownKeys = new HashSet<>();
        if (definitions != null) {
            for (ComputeVariableDefinition definition : definitions) {
                knownKeys.add(definition.variableKey());
            }
        }
        knownKeys.addAll(bindings.keySet());

        for (ComputeVariableDefinition definition : computeVars) {
            String key = definition.variableKey();
            String expression = definition.computeExpression();
            validateExpression(key, expression, knownKeys);
            expressions.put(key, expression.trim());
            try {
                ComputeExpressionParser parser = new ComputeExpressionParser(expression.trim());
                ComputeAst.Expr ast = parser.parseExpressionTree();
                Set<String> roots = ComputeExpressionEvaluator.collectVariableRoots(ast);
                Set<String> computeDeps = new HashSet<>();
                for (String root : roots) {
                    if (computeKeys.contains(root) && !root.equals(key)) {
                        computeDeps.add(root);
                    }
                }
                deps.put(key, computeDeps);
            } catch (ComputeExpressionLexSupport.ParseException ex) {
                throw new VariableComputeException(key, expression, ex.getMessage());
            }
        }

        List<String> order = topologicalOrder(computeVars, deps);
        Locale locale = resolveLocale(localeTag);
        for (String key : order) {
            String expression = expressions.get(key);
            try {
                ComputeExpressionParser parser = new ComputeExpressionParser(expression);
                ComputeAst.Expr ast = parser.parseExpressionTree();
                Object value = new ComputeExpressionEvaluator(bindings, locale, key, expression).evaluate(ast);
                bindings.put(key, value);
            } catch (ComputeExpressionLexSupport.ParseException ex) {
                throw new VariableComputeException(key, expression, ex.getMessage());
            }
        }
        return bindings;
    }

    private static List<String> topologicalOrder(
            List<ComputeVariableDefinition> computeVars,
            Map<String, Set<String>> deps
    ) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new HashMap<>();
        for (ComputeVariableDefinition definition : computeVars) {
            String key = definition.variableKey();
            indegree.putIfAbsent(key, 0);
            reverse.putIfAbsent(key, new ArrayList<>());
        }
        for (Map.Entry<String, Set<String>> entry : deps.entrySet()) {
            String key = entry.getKey();
            for (String dep : entry.getValue()) {
                reverse.computeIfAbsent(dep, ignored -> new ArrayList<>()).add(key);
                indegree.merge(key, 1, Integer::sum);
            }
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (ComputeVariableDefinition definition : computeVars) {
            String key = definition.variableKey();
            if (indegree.getOrDefault(key, 0) == 0) {
                queue.add(key);
            }
        }
        List<String> order = new ArrayList<>();
        Map<String, Integer> depth = new HashMap<>();
        while (!queue.isEmpty()) {
            String key = queue.removeFirst();
            order.add(key);
            int keyDepth = depth.getOrDefault(key, 0);
            if (keyDepth > ComputeDslLimits.MAX_DEPENDENCY_DEPTH) {
                throw new VariableComputeException(key, "", "dependency depth exceeds limit");
            }
            for (String dependent : reverse.getOrDefault(key, List.of())) {
                depth.merge(dependent, keyDepth + 1, Math::max);
                int next = indegree.merge(dependent, -1, Integer::sum);
                if (next == 0) {
                    queue.add(dependent);
                }
            }
        }
        if (order.size() != computeVars.size()) {
            String cycleKey = computeVars.stream()
                    .map(ComputeVariableDefinition::variableKey)
                    .filter(key -> !order.contains(key))
                    .findFirst()
                    .orElse("cycle");
            throw new VariableComputeException(
                    cycleKey,
                    "",
                    "Circular compute dependency detected"
            );
        }
        return order;
    }

    static Locale resolveLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return Locale.forLanguageTag(ComputeDslLimits.DEFAULT_LOCALE);
        }
        try {
            Locale locale = Locale.forLanguageTag(localeTag.trim());
            if (locale.getLanguage() == null || locale.getLanguage().isBlank()) {
                return Locale.forLanguageTag(ComputeDslLimits.DEFAULT_LOCALE);
            }
            return locale;
        } catch (RuntimeException ex) {
            return Locale.forLanguageTag(ComputeDslLimits.DEFAULT_LOCALE);
        }
    }
}
