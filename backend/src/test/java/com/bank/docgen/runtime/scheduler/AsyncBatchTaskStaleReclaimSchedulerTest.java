package com.bank.docgen.runtime.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.service.AsyncBatchTaskStaleReclaimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncBatchTaskStaleReclaimSchedulerTest {

    @Mock
    private AsyncBatchTaskStaleReclaimService staleReclaimService;

    @InjectMocks
    private AsyncBatchTaskStaleReclaimScheduler scheduler;

    @Test
    void tickDelegatesToReclaimService() {
        when(staleReclaimService.reclaimStaleTasks()).thenReturn(2);

        scheduler.reclaimStaleProcessingTasks();

        verify(staleReclaimService).reclaimStaleTasks();
    }
}
