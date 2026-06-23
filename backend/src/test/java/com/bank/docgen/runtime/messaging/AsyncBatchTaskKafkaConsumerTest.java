package com.bank.docgen.runtime.messaging;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.bank.docgen.runtime.service.AsyncBatchTaskRunner;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "test-kafka"})
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "generation.async-batch-task.v1",
                "generation.async-batch-task.v1.dlt"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AsyncBatchTaskKafkaConsumerTest {

    @Autowired
    private KafkaTemplate<String, AsyncBatchTaskMessage> asyncBatchTaskKafkaTemplate;

    @MockBean
    private AsyncBatchTaskRunner asyncBatchTaskRunner;

    @Test
    void consumerReceivesMessageAndInvokesRunner() throws Exception {
        UUID taskId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        asyncBatchTaskKafkaTemplate
                .send("generation.async-batch-task.v1", taskId.toString(), new AsyncBatchTaskMessage(taskId.toString()))
                .get(5, TimeUnit.SECONDS);

        verify(asyncBatchTaskRunner, timeout(15000)).processTask(taskId);
    }
}
