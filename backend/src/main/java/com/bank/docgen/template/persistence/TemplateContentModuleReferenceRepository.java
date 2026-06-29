package com.bank.docgen.template.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateContentModuleReferenceRepository extends JpaRepository<TemplateContentModuleReferenceEntity, UUID> {

    List<TemplateContentModuleReferenceEntity> findByTemplateVersionIdOrderByReferenceKeyAsc(UUID templateVersionId);

    Optional<TemplateContentModuleReferenceEntity> findByTemplateVersionIdAndReferenceKey(
            UUID templateVersionId,
            String referenceKey
    );

    List<TemplateContentModuleReferenceEntity> findByContentModuleVersionIdIn(Collection<UUID> contentModuleVersionIds);
}
