package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.RiskPromptScope;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskPromptConfigRepository extends JpaRepository<RiskPromptConfigEntity, UUID> {

    Optional<RiskPromptConfigEntity> findByScopeTypeAndGroupCode(RiskPromptScope scopeType, String groupCode);
}
