package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnchorBindingRepository extends JpaRepository<AnchorBindingEntity, UUID> {

    List<AnchorBindingEntity> findByTemplateVersionIdOrderByAnchorIdAsc(UUID templateVersionId);

    Optional<AnchorBindingEntity> findByTemplateVersionIdAndAnchorId(UUID templateVersionId, String anchorId);
}
