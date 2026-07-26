package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID>, TemplateRepositoryCustom {

    List<TemplateEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    Page<TemplateEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc(Pageable pageable);

    List<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List<String> groupCodes);

    Page<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
            List<String> groupCodes,
            Pageable pageable
    );

    Optional<TemplateEntity> findByIdAndDeletedAtIsNull(UUID id);

    /** FOS-W8-2: serialize publish / release stamp against concurrent writers. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TemplateEntity t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TemplateEntity> findByIdAndDeletedAtIsNullForUpdate(@Param("id") UUID id);

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

    List<TemplateEntity> findByDeletedAtIsNullAndNextReviewDueLessThanEqualOrderByNextReviewDueAscUpdatedAtDesc(
            LocalDate dueOnOrBefore
    );

    List<TemplateEntity> findByDeletedAtIsNullAndGroupCodeInAndNextReviewDueLessThanEqualOrderByNextReviewDueAscUpdatedAtDesc(
            List<String> groupCodes,
            LocalDate dueOnOrBefore
    );

    boolean existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNull(
            String groupCode,
            UUID localeVariantFamilyId,
            String locale
    );

    boolean existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNullAndIdNot(
            String groupCode,
            UUID localeVariantFamilyId,
            String locale,
            UUID id
    );
}
