package com.bank.docgen.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * LR-B2: proves the production ShedLock wiring (JdbcTemplateLockProvider + usingDbTime)
 * provides mutual exclusion, releases on unlock, and self-heals after lockAtMostFor
 * when a holder crashes without unlocking. Runs against the shared test database with
 * the shedlock table created from the real V46 migration DDL.
 */
@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JdbcTemplateLockProviderIntegrationTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private LockProvider lockProvider;

    @BeforeEach
    void setUpLockTable() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS shedlock");
        String migrationDdl = new ClassPathResource("db/migration/V46__shedlock.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute(migrationDdl);
        lockProvider = new SchedulerLockConfig().schedulerLockProvider(dataSource);
    }

    @Test
    void secondAcquisitionOfHeldLockReturnsEmpty() {
        Optional<SimpleLock> first = lockProvider.lock(lockConfig("invocation-retention-cleanup-artifacts"));
        assertThat(first).isPresent();

        Optional<SimpleLock> second = lockProvider.lock(lockConfig("invocation-retention-cleanup-artifacts"));
        assertThat(second).isEmpty();

        first.get().unlock();
    }

    @Test
    void lockIsReacquirableAfterRelease() {
        Optional<SimpleLock> first = lockProvider.lock(lockConfig("preview-temp-cleanup"));
        assertThat(first).isPresent();
        first.get().unlock();

        Optional<SimpleLock> second = lockProvider.lock(lockConfig("preview-temp-cleanup"));
        assertThat(second).isPresent();
        second.get().unlock();
    }

    @Test
    void expiredLockAtMostForAllowsNextAcquisitionWithoutUnlock() {
        // Simulates a crashed holder: never unlocked, very short lockAtMostFor.
        Optional<SimpleLock> crashed = lockProvider.lock(new LockConfiguration(
                Instant.now(),
                "collaboration-escalation-check",
                Duration.ofMillis(200),
                Duration.ZERO
        ));
        assertThat(crashed).isPresent();

        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)).until(() -> {
            Optional<SimpleLock> next = lockProvider.lock(lockConfig("collaboration-escalation-check"));
            next.ifPresent(SimpleLock::unlock);
            return next.isPresent();
        });
    }

    private LockConfiguration lockConfig(String name) {
        return new LockConfiguration(Instant.now(), name, Duration.ofMinutes(10), Duration.ZERO);
    }
}
