package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    List<TemplateEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    Page<TemplateEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);

    List<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List<String> groupCodes);

    Page<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
            List<String> groupCodes,
            Pageable pageable
    );

    Optional<TemplateEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<TemplateEntity> findByExternalIdAndDeletedAtIsNull(String externalId);

    List<TemplateEntity> findByIdInAndDeletedAtIsNull(List<UUID> ids);

    List<TemplateEntity> findByMasterIdAndDeletedAtIsNull(UUID masterId);

    List<TemplateEntity> findByDeletedAtIsNullAndLifecycleStatusOrderByUpdatedAtDesc(
            TemplateLifecycleStatus lifecycleStatus
    );

    List<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
            List<String> groupCodes,
            TemplateLifecycleStatus lifecycleStatus
    );
}
