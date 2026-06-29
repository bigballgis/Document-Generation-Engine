package com.bank.docgen.contentmodule.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentModuleRepository extends JpaRepository<ContentModuleEntity, UUID> {

    Optional<ContentModuleEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<ContentModuleEntity> findByGroupCodeAndDeletedAtIsNull(String groupCode);

    List<ContentModuleEntity> findByGroupCodeInAndDeletedAtIsNullOrderByUpdatedAtDesc(List<String> groupCodes);

    List<ContentModuleEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    Optional<ContentModuleEntity> findByModuleCodeAndDeletedAtIsNull(String moduleCode);

    boolean existsByModuleCodeAndDeletedAtIsNull(String moduleCode);
}
