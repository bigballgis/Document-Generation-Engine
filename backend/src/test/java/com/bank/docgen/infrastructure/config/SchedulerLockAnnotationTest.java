package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.collaboration.scheduler.CollaborationEscalationScheduler;
import com.bank.docgen.rendering.scheduler.PreviewTempCleanupScheduler;
import com.bank.docgen.runtime.scheduler.AsyncBatchTaskStaleReclaimScheduler;
import com.bank.docgen.runtime.scheduler.InvocationRetentionCleanupScheduler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * LR-B2 regression guard: every scheduled job must keep its ShedLock mutex annotation
 * (ADR-0044 scale-out prerequisite #1; also covers restart / blue-green overlap windows).
 */
class SchedulerLockAnnotationTest {

    private static final List<Class<?>> SCHEDULER_CLASSES = List.of(
            InvocationRetentionCleanupScheduler.class,
            CollaborationEscalationScheduler.class,
            PreviewTempCleanupScheduler.class,
            AsyncBatchTaskStaleReclaimScheduler.class
    );

    @Test
    void everyScheduledMethodCarriesSchedulerLock() {
        List<Method> scheduledMethods = scheduledMethods();

        assertThat(scheduledMethods).isNotEmpty();
        for (Method method : scheduledMethods) {
            assertThat(method.getAnnotation(SchedulerLock.class))
                    .as("@SchedulerLock missing on %s#%s", method.getDeclaringClass().getSimpleName(), method.getName())
                    .isNotNull();
        }
    }

    @Test
    void schedulerLockNamesAreUniqueAcrossAllJobs() {
        List<String> names = scheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .filter(lock -> lock != null)
                .map(SchedulerLock::name)
                .toList();

        assertThat(names).hasSameSizeAs(scheduledMethods());
        assertThat(names).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(names).allSatisfy(name -> assertThat(name).isNotBlank());
    }

    @Test
    void lockDurationsBoundExecutionAndStayBelowTriggerIntervals() {
        for (Method method : scheduledMethods()) {
            SchedulerLock lock = method.getAnnotation(SchedulerLock.class);
            assertThat(lock).isNotNull();
            Duration atMost = Duration.parse(lock.lockAtMostFor());
            Duration atLeast = Duration.parse(lock.lockAtLeastFor());
            // These jobs are fast (seconds); PT10M is a safe crash-recovery upper bound.
            assertThat(atMost)
                    .as("lockAtMostFor on %s", lock.name())
                    .isEqualTo(Duration.ofMinutes(10));
            // Shortest trigger interval across the three schedulers is 5 minutes
            // (docgen.collaboration.escalation.fixed-delay-ms default 300000).
            assertThat(atLeast)
                    .as("lockAtLeastFor on %s must stay far below the trigger interval", lock.name())
                    .isPositive()
                    .isLessThan(Duration.ofMinutes(1));
        }
    }

    private List<Method> scheduledMethods() {
        return SCHEDULER_CLASSES.stream()
                .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .toList();
    }
}
