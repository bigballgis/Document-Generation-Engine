package com.bank.docgen.apimgmt.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiPolicyRepository extends JpaRepository<ApiPolicyEntity, UUID> {

    Optional<ApiPolicyEntity> findByTemplateId(UUID templateId);
}
