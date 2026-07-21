package com.bank.docgen.library.persistence;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Split q/no-q queries: PostgreSQL type-checks OR branches even when {@code :q IS NULL},
 * and Hibernate binds a null String as bytea → {@code lower(bytea)} at runtime.
 */
public interface LibraryAssetRepository extends JpaRepository<LibraryAssetEntity, LibraryAssetId> {

    Optional<LibraryAssetEntity> findByGroupCodeAndAssetKeyAndDeletedAtIsNull(String groupCode, String assetKey);

    List<LibraryAssetEntity> findByMigratedQuarantineAtIsNotNullAndObjectPurgeCompletedAtIsNullAndDeletedAtIsNull();

    @Query("""
            SELECT a FROM LibraryAssetEntity a
            WHERE a.deletedAt IS NULL
              AND (:status IS NULL OR a.status = :status)
              AND (:assetClass IS NULL OR a.assetClass = :assetClass)
              AND (:groupCode IS NULL OR a.groupCode = :groupCode)
              AND (:restrictGroups = FALSE OR a.groupCode IN :accessibleGroups)
            """)
    Page<LibraryAssetEntity> search(
            @Param("status") AssetLibraryAssetStatus status,
            @Param("assetClass") AssetLibraryAssetClass assetClass,
            @Param("groupCode") String groupCode,
            @Param("restrictGroups") boolean restrictGroups,
            @Param("accessibleGroups") Collection<String> accessibleGroups,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM LibraryAssetEntity a
            WHERE a.deletedAt IS NULL
              AND (:status IS NULL OR a.status = :status)
              AND (:assetClass IS NULL OR a.assetClass = :assetClass)
              AND (:groupCode IS NULL OR a.groupCode = :groupCode)
              AND (:restrictGroups = FALSE OR a.groupCode IN :accessibleGroups)
              AND (
                    LOWER(a.assetKey) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(a.originalFileName) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<LibraryAssetEntity> searchByQuery(
            @Param("status") AssetLibraryAssetStatus status,
            @Param("assetClass") AssetLibraryAssetClass assetClass,
            @Param("groupCode") String groupCode,
            @Param("restrictGroups") boolean restrictGroups,
            @Param("accessibleGroups") Collection<String> accessibleGroups,
            @Param("q") String q,
            Pageable pageable
    );
}
