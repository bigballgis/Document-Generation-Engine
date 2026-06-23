package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestDataSetRepository extends JpaRepository<TestDataSetEntity, UUID> {

    List<TestDataSetEntity> findByTemplateIdOrderByUpdatedAtDesc(UUID templateId);

    Optional<TestDataSetEntity> findByTemplateIdAndExternalId(UUID templateId, String externalId);
}
