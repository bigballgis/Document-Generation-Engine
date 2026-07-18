package com.bank.docgen.template.service;

import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * IBL-A1 shared VariableSchema validation for runtime generate and management preview.
 */
@Service
public class VariableSchemaValidationService implements VariableSchemaValidationPort {

    private final VariableSchemaRepository variableSchemaRepository;

    public VariableSchemaValidationService(VariableSchemaRepository variableSchemaRepository) {
        this.variableSchemaRepository = variableSchemaRepository;
    }

    @Override
    public void validateForAssembly(UUID templateVersionId, Map<String, Object> variables) {
        List<VariableSchemaEntity> schema = variableSchemaRepository
                .findByTemplateVersionIdOrderByVariableKeyAsc(templateVersionId);
        VariableSchemaPayloadValidator.validateOrThrow(schema, variables);
    }
}
