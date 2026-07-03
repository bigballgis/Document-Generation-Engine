package com.bank.docgen.authorization.management.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementUserRepository extends JpaRepository<ManagementUserEntity, UUID> {

    @EntityGraph(attributePaths = {"roles", "authorizedGroupCodes"})
    Optional<ManagementUserEntity> findByUsernameAndDeletedAtIsNull(String username);

    @EntityGraph(attributePaths = {"roles", "authorizedGroupCodes"})
    Optional<ManagementUserEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "authorizedGroupCodes"})
    List<ManagementUserEntity> findByDeletedAtIsNullOrderByUsernameAsc();
}
