package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * BDD-LRP-D1-005/006 — scheduler lock + retention-enabled gate (ADR-0048 / LR-B2).
 */
@ExtendWith(MockitoExtension.class)
class AuditRetentionCleanupSchedulerTest {

    @Mock
    private AuditRetentionCleanupService cleanupService;

    private AuditRetentionCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AuditRetentionCleanupScheduler(cleanupService, true);
    }

    @Test
    void cleanupManagement_delegatesWhenEnabled() {
        when(cleanupService.purgeManagementAudit()).thenReturn(2);

        scheduler.cleanupExpiredManagementAudit();

        verify(cleanupService).purgeManagementAudit();
    }

    @Test
    void cleanupRuntime_delegatesWhenEnabled() {
        when(cleanupService.purgeRuntimeAudit()).thenReturn(1);

        scheduler.cleanupExpiredRuntimeAudit();

        verify(cleanupService).purgeRuntimeAudit();
    }

    @Test
    void cleanupSkipsServiceWhenRetentionDisabled() {
        scheduler = new AuditRetentionCleanupScheduler(cleanupService, false);

        scheduler.cleanupExpiredManagementAudit();
        scheduler.cleanupExpiredRuntimeAudit();

        verifyNoInteractions(cleanupService);
    }

    @Test
    void dualSchedulerLocksMatchConfirmedNamesAndDurations() throws Exception {
        Method management = AuditRetentionCleanupScheduler.class.getMethod("cleanupExpiredManagementAudit");
        Method runtime = AuditRetentionCleanupScheduler.class.getMethod("cleanupExpiredRuntimeAudit");

        assertThat(management.getAnnotation(Scheduled.class)).isNotNull();
        assertThat(runtime.getAnnotation(Scheduled.class)).isNotNull();

        SchedulerLock managementLock = management.getAnnotation(SchedulerLock.class);
        SchedulerLock runtimeLock = runtime.getAnnotation(SchedulerLock.class);
        assertThat(managementLock).isNotNull();
        assertThat(runtimeLock).isNotNull();
        assertThat(managementLock.name()).isEqualTo("audit-retention-cleanup-management");
        assertThat(runtimeLock.name()).isEqualTo("audit-retention-cleanup-runtime");
        assertThat(Duration.parse(managementLock.lockAtMostFor())).isEqualTo(Duration.ofMinutes(10));
        assertThat(Duration.parse(runtimeLock.lockAtMostFor())).isEqualTo(Duration.ofMinutes(10));
        assertThat(Duration.parse(managementLock.lockAtLeastFor())).isEqualTo(Duration.ofSeconds(20));
        assertThat(Duration.parse(runtimeLock.lockAtLeastFor())).isEqualTo(Duration.ofSeconds(20));
        assertThat(managementLock.name()).isNotEqualTo(runtimeLock.name());
    }

    @Test
    void scheduledMethodsAllCarryUniqueLocks() {
        Method[] methods = Arrays.stream(AuditRetentionCleanupScheduler.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class))
                .toArray(Method[]::new);

        assertThat(methods).hasSize(2);
        assertThat(Arrays.stream(methods).map(method -> method.getAnnotation(SchedulerLock.class).name()))
                .doesNotHaveDuplicates();
    }
}
