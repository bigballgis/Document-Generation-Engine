package com.bank.docgen.master.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterDocumentRepository extends JpaRepository<MasterDocumentEntity, UUID> {

    List<MasterDocumentEntity> findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(List<String> groupCodes);

    List<MasterDocumentEntity> findByDeletedAtIsNullOrderByUpdatedAtDesc();

    Optional<MasterDocumentEntity> findByIdAndDeletedAtIsNull(UUID id);
}
