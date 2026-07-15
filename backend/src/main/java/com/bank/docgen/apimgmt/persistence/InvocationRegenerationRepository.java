package com.bank.docgen.apimgmt.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvocationRegenerationRepository extends JpaRepository<InvocationRegenerationEntity, UUID> {

    Optional<InvocationRegenerationEntity> findByRegenerationExternalId(String regenerationExternalId);
}
