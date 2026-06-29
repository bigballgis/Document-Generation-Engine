package com.bank.docgen.rendering.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchTestRunRepository extends JpaRepository<BatchTestRunEntity, UUID> {

    List<BatchTestRunEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);
}
