package com.bank.docgen.authorization.management.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementUserRepository extends JpaRepository<ManagementUserEntity, UUID> {

    Optional<ManagementUserEntity> findByUsernameAndDeletedAtIsNull(String username);

    Optional<ManagementUserEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUsername(String username);

    List<ManagementUserEntity> findByDeletedAtIsNullOrderByUsernameAsc();
}
