package com.bank.docgen.legalhold.persistence;

import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalHoldRepository extends JpaRepository<LegalHoldEntity, UUID> {

    List<LegalHoldEntity> findByStatus(LegalHoldStatus status);

    List<LegalHoldEntity> findAllByOrderByCreatedAtDesc();

    List<LegalHoldEntity> findByStatusOrderByCreatedAtDesc(LegalHoldStatus status);

    Optional<LegalHoldEntity> findByHoldExternalId(String holdExternalId);
}
