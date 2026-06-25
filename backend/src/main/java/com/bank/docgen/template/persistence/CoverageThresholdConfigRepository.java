package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.CoverageThresholdScope;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverageThresholdConfigRepository extends JpaRepository<CoverageThresholdConfigEntity, UUID> {

    Optional<CoverageThresholdConfigEntity> findByScopeTypeAndGroupCode(
            CoverageThresholdScope scopeType,
            String groupCode
    );
}
