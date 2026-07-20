package com.bank.docgen.contentmodule.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentModuleNestingEdgeRepository extends JpaRepository<ContentModuleNestingEdgeEntity, UUID> {

    void deleteByParentVersionId(UUID parentVersionId);

    List<ContentModuleNestingEdgeEntity> findByParentVersionId(UUID parentVersionId);

    List<ContentModuleNestingEdgeEntity> findByTargetModuleId(UUID targetModuleId);
}
