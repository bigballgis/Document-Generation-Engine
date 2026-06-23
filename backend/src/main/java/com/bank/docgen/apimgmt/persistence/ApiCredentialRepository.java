package com.bank.docgen.apimgmt.persistence;

import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCredentialRepository extends JpaRepository<ApiCredentialEntity, UUID> {

    Optional<ApiCredentialEntity> findByExternalId(String externalId);

    List<ApiCredentialEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    List<ApiCredentialEntity> findByTemplateIdAndStatusOrderByCreatedAtDesc(
            UUID templateId,
            ApiCredentialStatus status
    );
}
