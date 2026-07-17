package com.bank.docgen.contentmodule.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Locale;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G05 — keep {@code content_module_version.content_search_vector} in sync (PostgreSQL).
 * No-ops on H2 test databases where tsvector is unavailable.
 */
@Component
public class ContentModuleFullTextIndexWriter {

    private static final Logger LOG = LoggerFactory.getLogger(ContentModuleFullTextIndexWriter.class);

    private final ContentModuleSearchableTextExtractor extractor;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public ContentModuleFullTextIndexWriter(
            ContentModuleSearchableTextExtractor extractor,
            DataSource dataSource
    ) {
        this.extractor = extractor;
        this.dataSource = dataSource;
    }

    @Transactional
    public void refresh(UUID versionId, String contentStructureJson) {
        if (versionId == null || !isPostgres()) {
            return;
        }
        String searchable = extractor.extract(contentStructureJson);
        entityManager.createNativeQuery(
                        """
                        UPDATE content_module_version
                        SET content_search_vector = to_tsvector('simple', :searchable)
                        WHERE id = :versionId
                        """
                )
                .setParameter("searchable", searchable)
                .setParameter("versionId", versionId)
                .executeUpdate();
    }

    private boolean isPostgres() {
        try (var connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("postgres");
        } catch (Exception ex) {
            LOG.warn("CE-G05 FTS dialect probe failed: {}", ex.getMessage());
            return false;
        }
    }
}
