package com.bank.docgen.master.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MasterRevisionLineRepository extends JpaRepository<MasterRevisionLineEntity, UUID> {

    Page<MasterRevisionLineEntity> findByMasterIdAndDeletedAtIsNullOrderByCurrentDescCreatedAtDesc(
            UUID masterId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "anchors")
    Optional<MasterRevisionLineEntity> findByIdAndMasterIdAndDeletedAtIsNull(UUID id, UUID masterId);

    Optional<MasterRevisionLineEntity> findByMasterIdAndCurrentTrueAndDeletedAtIsNull(UUID masterId);

    @Query("""
            SELECT COALESCE(MAX(line.revisionSequence), 0)
            FROM MasterRevisionLineEntity line
            WHERE line.masterId = :masterId AND line.deletedAt IS NULL
            """)
    int findMaxRevisionSequence(@Param("masterId") UUID masterId);

    @Query("""
            SELECT line.createdAt
            FROM MasterRevisionLineEntity line
            WHERE line.masterId = :masterId
              AND line.revisionSequence = :sequence
              AND line.deletedAt IS NULL
            """)
    Optional<java.time.Instant> findCreatedAtByMasterIdAndRevisionSequence(
            @Param("masterId") UUID masterId,
            @Param("sequence") int sequence
    );
}
