package com.bank.docgen.authorization.management.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessGroupRepository extends JpaRepository<BusinessGroupEntity, UUID> {

    Optional<BusinessGroupEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BusinessGroupEntity> findByGroupCodeAndDeletedAtIsNull(String groupCode);

    boolean existsByGroupCodeAndDeletedAtIsNull(String groupCode);

    List<BusinessGroupEntity> findByDeletedAtIsNullOrderByGroupCodeAsc();
}
