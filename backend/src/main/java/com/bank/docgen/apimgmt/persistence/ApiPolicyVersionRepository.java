package com.bank.docgen.apimgmt.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiPolicyVersionRepository extends JpaRepository<ApiPolicyVersionEntity, UUID> {

    List<ApiPolicyVersionEntity> findByTemplateIdOrderByPolicyVersionAsc(UUID templateId);

    Optional<ApiPolicyVersionEntity> findByTemplateIdAndPolicyVersion(UUID templateId, int policyVersion);
}
