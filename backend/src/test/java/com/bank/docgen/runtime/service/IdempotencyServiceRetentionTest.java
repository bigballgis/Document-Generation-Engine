package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceRetentionTest {

    private static final String KEY = "idem-retention";
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
    void beginUsesSevenDayRetentionWindow() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        service.begin(KEY, TEMPLATE_ID, "hash-1");
        Instant after = Instant.now();

        ArgumentCaptor<GenerationIdempotencyEntity> captor =
                ArgumentCaptor.forClass(GenerationIdempotencyEntity.class);
        verify(repository).saveAndFlush(captor.capture());

        Instant expiresAt = captor.getValue().getExpiresAt();
        long retentionSeconds = ChronoUnit.SECONDS.between(before, expiresAt);
        assertThat(retentionSeconds).isBetween(
                IdempotencyConstants.RETENTION_SECONDS - 5,
                IdempotencyConstants.RETENTION_SECONDS + 5
        );
    }
}
