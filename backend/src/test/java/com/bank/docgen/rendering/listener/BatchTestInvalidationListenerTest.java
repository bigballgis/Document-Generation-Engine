package com.bank.docgen.rendering.listener;

import static org.mockito.Mockito.verify;

import com.bank.docgen.rendering.service.BatchTestInvalidationService;
import com.bank.docgen.template.event.TemplateContentChangedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchTestInvalidationListenerTest {

    @Mock
    private BatchTestInvalidationService batchTestInvalidationService;

    @InjectMocks
    private BatchTestInvalidationListener listener;

    @Test
    void onTemplateContentChanged_delegatesToInvalidationService() {
        UUID templateId = UUID.randomUUID();
        TemplateContentChangedEvent event = new TemplateContentChangedEvent(this, templateId);

        listener.onTemplateContentChanged(event);

        verify(batchTestInvalidationService).invalidateLatestRun(templateId);
    }
}
