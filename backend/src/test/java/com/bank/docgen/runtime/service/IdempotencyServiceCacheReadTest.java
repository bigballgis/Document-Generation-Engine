package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceCacheReadTest {

    private static final String KEY = "idem-key-cache";
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CACHE_KEY = TEMPLATE_ID + ":" + KEY;

    @Mock
    private GenerationIdempotencyRepository repository;

    @Mock
    private IdempotencyCachePort cachePort;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, cachePort);
    }

    @Test
    void cacheHitWithDifferentHashRaisesConflictWithoutDatabaseRead() {
        when(cachePort.findRequestHash(CACHE_KEY)).thenReturn(Optional.of("hash-A"));

        assertThatThrownBy(() -> service.findExisting(KEY, TEMPLATE_ID, "hash-B"))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(repository, never()).findByIdempotencyKeyAndTemplateId(any(), any());
    }

    @Test
    void cacheMissFallsBackToDatabaseAndWarmsCache() {
        GenerationIdempotencyEntity existing = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                KEY,
                TEMPLATE_ID,
                "hash-A",
                "COMPLETED",
                Instant.now().plusSeconds(3600)
        );
        when(cachePort.findRequestHash(CACHE_KEY)).thenReturn(Optional.empty());
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(existing));

        Optional<GenerationIdempotencyEntity> result = service.findExisting(KEY, TEMPLATE_ID, "hash-A");

        assertThat(result).containsSame(existing);
        verify(cachePort).remember(eq(CACHE_KEY), eq("hash-A"), eq(existing.getExpiresAt()));
    }

    @Test
    void cacheHitWithMatchingHashStillLoadsAuthoritativeRecordFromDatabase() {
        GenerationIdempotencyEntity existing = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                KEY,
                TEMPLATE_ID,
                "hash-A",
                "COMPLETED",
                Instant.now().plusSeconds(3600)
        );
        when(cachePort.findRequestHash(CACHE_KEY)).thenReturn(Optional.of("hash-A"));
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(existing));

        Optional<GenerationIdempotencyEntity> result = service.findExisting(KEY, TEMPLATE_ID, "hash-A");

        assertThat(result).containsSame(existing);
        verify(cachePort, never()).remember(any(), any(), any());
    }
}
