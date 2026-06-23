package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableSchemaRepository extends JpaRepository<VariableSchemaEntity, UUID> {

    List<VariableSchemaEntity> findByTemplateVersionIdOrderByVariableKeyAsc(UUID templateVersionId);

    Optional<VariableSchemaEntity> findByTemplateVersionIdAndVariableKey(UUID templateVersionId, String variableKey);
}
