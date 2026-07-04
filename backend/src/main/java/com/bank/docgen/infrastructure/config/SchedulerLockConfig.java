package com.bank.docgen.infrastructure.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * LR-B2 (ADR-0044): distributed mutex for {@code @Scheduled} jobs, backed by the
 * Flyway-managed {@code shedlock} table (V46). JDBC-backed on purpose — these jobs are
 * DB-centric, so the lock must not fail open when Redis is down. {@code usingDbTime()}
 * removes clock-skew sensitivity between instances during restart / blue-green overlap.
 *
 * <p>Profile mirrors {@link CollaborationSchedulingConfig}: the test profile runs
 * schedulers as plain beans without scheduling or lock proxies.</p>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@Profile("!test")
public class SchedulerLockConfig {

    @Bean
    public LockProvider schedulerLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}
