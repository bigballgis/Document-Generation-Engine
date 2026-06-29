package com.bank.docgen.collaboration.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.collaboration.service.CollaborationEscalationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationEscalationSchedulerTest {

    @Mock
    private CollaborationEscalationService escalationService;

    @InjectMocks
    private CollaborationEscalationScheduler scheduler;

    @Test
    void runEscalationCheck_delegatesToService() {
        when(escalationService.processDueEscalations()).thenReturn(2);

        scheduler.runEscalationCheck();

        verify(escalationService).processDueEscalations();
    }
}
