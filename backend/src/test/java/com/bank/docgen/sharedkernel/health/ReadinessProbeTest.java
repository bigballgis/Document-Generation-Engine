package com.bank.docgen.sharedkernel.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ReadinessProbeTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void postgresDownReturns503Status() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("connection refused"));

        ReadinessProbe probe = new ReadinessProbe(jdbcTemplate, List.of());
        ReadinessReport report = probe.check();

        assertThat(report.status()).isEqualTo("DOWN");
        assertThat(report.checks().get("postgres").status()).isEqualTo("DOWN");
        assertThat(report.trafficReady()).isFalse();
    }

    @Test
    void postgresUpRedisDownReturns200WithDegradedDetail() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        ComponentReadinessContributor redisContributor = new ComponentReadinessContributor() {
            @Override
            public String componentName() {
                return "redis";
            }

            @Override
            public ComponentCheck check() {
                return new ComponentCheck("DOWN", null);
            }
        };

        ReadinessProbe probe = new ReadinessProbe(jdbcTemplate, List.of(redisContributor));
        ReadinessReport report = probe.check();

        assertThat(report.status()).isEqualTo("UP");
        assertThat(report.trafficReady()).isTrue();
        assertThat(report.checks().get("postgres").status()).isEqualTo("UP");
        assertThat(report.checks().get("redis").status()).isEqualTo("DOWN");
    }

    @Test
    void allContributorsUpReturnsFullChecksMap() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        ComponentReadinessContributor redis = namedContributor("redis", "UP", null);
        ComponentReadinessContributor minio = namedContributor("minio", "UP", null);
        ComponentReadinessContributor kafka = namedContributor("kafka", "SKIPPED", "transport not kafka");

        ReadinessProbe probe = new ReadinessProbe(jdbcTemplate, List.of(redis, minio, kafka));
        ReadinessReport report = probe.check();

        assertThat(report.status()).isEqualTo("UP");
        assertThat(report.checks()).containsKeys("postgres", "redis", "minio", "kafka");
    }

    private static ComponentReadinessContributor namedContributor(String name, String status, String detail) {
        return new ComponentReadinessContributor() {
            @Override
            public String componentName() {
                return name;
            }

            @Override
            public ComponentCheck check() {
                return new ComponentCheck(status, detail);
            }
        };
    }
}
