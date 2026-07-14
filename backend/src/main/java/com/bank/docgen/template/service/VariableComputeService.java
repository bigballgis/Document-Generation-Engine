package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.document.compute.ComputeVariableDefinition;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeEngine;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.port.VariableComputePort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Loads variable schemas and applies whitelist compute evaluation before DOCX assembly (CE-K03).
 */
@Service
public class VariableComputeService implements VariableComputePort {

    private final VariableSchemaRepository variableSchemaRepository;
    private final VariableComputeEngine engine = VariableComputeEngine.INSTANCE;

    public VariableComputeService(VariableSchemaRepository variableSchemaRepository) {
        this.variableSchemaRepository = variableSchemaRepository;
    }

    @Override
    public Map<String, Object> applyCompute(
            UUID templateVersionId,
            Map<String, Object> inputVariables,
            String localeTag
    ) {
        List<VariableSchemaEntity> schemas =
                variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(templateVersionId);
        List<ComputeVariableDefinition> definitions = toDefinitions(schemas);
        return engine.evaluateAll(definitions, inputVariables, localeTag);
    }

    public void validateComputeExpression(
            UUID templateVersionId,
            String variableKey,
            String expression,
            String currentKeyBeingEdited
    ) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        Set<String> knownKeys = variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(templateVersionId)
                .stream()
                .map(VariableSchemaEntity::getVariableKey)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (currentKeyBeingEdited != null && !currentKeyBeingEdited.isBlank()) {
            knownKeys.add(currentKeyBeingEdited);
        }
        if (variableKey != null && !variableKey.isBlank()) {
            knownKeys.add(variableKey);
        }
        try {
            engine.validateExpression(variableKey, expression, knownKeys);
        } catch (com.bank.docgen.sharedkernel.document.compute.VariableComputeException ex) {
            throw new TemplateValidationException("api.error.template.computeExpressionInvalid");
        }
    }

    public Object evaluateSample(
            String variableKey,
            String expression,
            Map<String, Object> sampleVariables,
            String localeTag
    ) {
        return engine.evaluateSingle(variableKey, expression, sampleVariables, localeTag);
    }

    public void validateExpressionAgainstKeys(String variableKey, String expression, Set<String> knownKeys) {
        engine.validateExpression(variableKey, expression, knownKeys);
    }

    private static List<ComputeVariableDefinition> toDefinitions(List<VariableSchemaEntity> schemas) {
        List<ComputeVariableDefinition> definitions = new ArrayList<>();
        for (VariableSchemaEntity schema : schemas) {
            definitions.add(new ComputeVariableDefinition(
                    schema.getVariableKey(),
                    schema.getComputeExpression(),
                    schema.getVariableType() == VariableType.COMPUTED
            ));
        }
        return definitions;
    }

    public Map<String, Object> evaluateAllFromDefinitions(
            List<ComputeVariableDefinition> definitions,
            Map<String, Object> inputVariables,
            String localeTag
    ) {
        Map<String, Object> safe = inputVariables == null ? Map.of() : new LinkedHashMap<>(inputVariables);
        return engine.evaluateAll(definitions, safe, localeTag);
    }
}
