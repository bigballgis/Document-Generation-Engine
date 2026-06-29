package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaborationTimeoutConfigRepository extends JpaRepository<CollaborationTimeoutConfigEntity, UUID> {

    Optional<CollaborationTimeoutConfigEntity> findByScopeTypeAndGroupCode(
            CollaborationTimeoutScope scopeType,
            String groupCode
    );
}
