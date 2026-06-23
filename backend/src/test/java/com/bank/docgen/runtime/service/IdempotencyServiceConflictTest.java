package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceConflictTest {

    private static final String KEY = "idem-key-1";
    private static final UUID TEMPLATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private GenerationIdempotencyRepository repository;

    @Mock
    private IdempotencyCachePort cachePort;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, cachePort);
        lenient().when(cachePort.findRequestHash(any())).thenReturn(Optional.empty());
    }

    private GenerationIdempotencyEntity record(String requestHash, Instant expiresAt) {
        return new GenerationIdempotencyEntity(
                UUID.randomUUID(), KEY, TEMPLATE_ID, requestHash, "COMPLETED", expiresAt);
    }

    @Test
    void liveRecordWithDifferentHashRaisesConflict() {
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(record("hash-A", Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> service.findExisting(KEY, TEMPLATE_ID, "hash-B"))
                .isInstanceOf(IdempotencyConflictException.class)
                .satisfies(ex -> assertThat(((IdempotencyConflictException) ex).conflictType())
                        .isEqualTo(IdempotencyConflictException.REQUEST_SEMANTICS_MISMATCH));
    }

    @Test
    void liveRecordWithSameHashReplays() {
        GenerationIdempotencyEntity existing = record("hash-A", Instant.now().plusSeconds(3600));
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(existing));

        Optional<GenerationIdempotencyEntity> result = service.findExisting(KEY, TEMPLATE_ID, "hash-A");

        assertThat(result).containsSame(existing);
    }

    @Test
    void expiredRecordWithDifferentHashIsTreatedAsNew() {
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(record("hash-A", Instant.now().minusSeconds(60))));

        Optional<GenerationIdempotencyEntity> result = service.findExisting(KEY, TEMPLATE_ID, "hash-B");

        assertThat(result).isEmpty();
    }

    @Test
    void concurrentInsertWithDifferentHashRaisesConflictNotDataIntegrityError() {
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(record("hash-A", Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> service.begin(KEY, TEMPLATE_ID, "hash-B"))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void concurrentInsertWithSameHashReturnsExistingRecord() {
        GenerationIdempotencyEntity existing = record("hash-A", Instant.now().plusSeconds(3600));
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));
        when(repository.findByIdempotencyKeyAndTemplateId(KEY, TEMPLATE_ID))
                .thenReturn(Optional.of(existing));

        GenerationIdempotencyEntity result = service.begin(KEY, TEMPLATE_ID, "hash-A");

        assertThat(result).isSameAs(existing);
    }
}
