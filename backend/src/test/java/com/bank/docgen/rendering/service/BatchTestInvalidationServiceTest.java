package com.bank.docgen.rendering.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BatchTestInvalidationServiceTest {

    @Mock
    private BatchTestRunRepository batchTestRunRepository;

    @InjectMocks
    private BatchTestInvalidationService service;

    @Test
    void invalidateLatestRun_whenValidRunExists_setsInvalidatedAt() {
        UUID templateId = UUID.randomUUID();
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                UUID.randomUUID(), templateId, UUID.randomUUID(), "author", 3
        );
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId))
                .thenReturn(Optional.of(run));
        when(batchTestRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.invalidateLatestRun(templateId);

        ArgumentCaptor<BatchTestRunEntity> captor = ArgumentCaptor.forClass(BatchTestRunEntity.class);
        verify(batchTestRunRepository).save(captor.capture());
        assertThat(captor.getValue().getInvalidatedAt()).isNotNull();
    }

    @Test
    void invalidateLatestRun_whenNoValidRun_doesNothing() {
        UUID templateId = UUID.randomUUID();
        when(batchTestRunRepository.findLatestValidByTemplateId(templateId))
                .thenReturn(Optional.empty());

        service.invalidateLatestRun(templateId);

        verify(batchTestRunRepository, never()).save(any());
    }
}
