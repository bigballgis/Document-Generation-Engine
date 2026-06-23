package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    List<TemplateEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    List<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List<String> groupCodes);

    Optional<TemplateEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<TemplateEntity> findByExternalIdAndDeletedAtIsNull(String externalId);

    List<TemplateEntity> findByMasterIdAndDeletedAtIsNull(UUID masterId);
}
