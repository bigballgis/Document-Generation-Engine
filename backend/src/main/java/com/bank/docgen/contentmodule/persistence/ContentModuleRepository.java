package com.bank.docgen.contentmodule.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentModuleRepository extends JpaRepository<ContentModuleEntity, UUID>, ContentModuleRepositoryCustom {

    Optional<ContentModuleEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<ContentModuleEntity> findByGroupCodeAndDeletedAtIsNull(String groupCode);

    List<ContentModuleEntity> findByGroupCodeInAndDeletedAtIsNullOrderByUpdatedAtDesc(List<String> groupCodes);

    List<ContentModuleEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    Optional<ContentModuleEntity> findByModuleCodeAndDeletedAtIsNull(String moduleCode);

    boolean existsByModuleCodeAndDeletedAtIsNull(String moduleCode);

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
