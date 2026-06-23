package com.bank.docgen.authorization.management.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementUserRepository extends JpaRepository<ManagementUserEntity, UUID> {

    Optional<ManagementUserEntity> findByUsernameAndDeletedAtIsNull(String username);
}
