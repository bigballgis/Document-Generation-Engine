package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.async.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class AsyncConfigMdcDecoratorTest {

    @Test
    void nonTestAsyncTaskExecutorWiresMdcTaskDecorator() {
        Executor executor = new AsyncConfig().asyncTaskExecutor();
        try {
            assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
            TaskDecorator decorator =
                    (TaskDecorator) ReflectionTestUtils.getField(executor, "taskDecorator");
            assertThat(decorator).isInstanceOf(MdcTaskDecorator.class);
        } finally {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    }
}
