package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, UUID> {

    Optional<TemplateVersionEntity> findByTemplateIdAndDevVersionNumber(UUID templateId, int devVersionNumber);

    List<TemplateVersionEntity> findByTemplateIdOrderByDevVersionNumberDesc(UUID templateId);

    Optional<TemplateVersionEntity> findByTemplateIdAndReleaseVersion(UUID templateId, String releaseVersion);
}
