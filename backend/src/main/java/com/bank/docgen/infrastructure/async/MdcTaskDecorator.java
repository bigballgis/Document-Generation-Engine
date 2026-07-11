package com.bank.docgen.infrastructure.async;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Copies caller-thread MDC (including {@code traceId}) onto async worker threads and clears after the task.
 * ADR-0049 / LR-D4.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();
        return () -> {
            if (callerContext != null) {
                MDC.setContextMap(callerContext);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
