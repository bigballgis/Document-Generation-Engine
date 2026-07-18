package com.bank.docgen.infrastructure.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BDD-PRR-D01B-001…005 — hardening path: metrics/prometheus require HTTP Basic;
 * healthz/readyz stay public.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "docgen.actuator.scrape-auth-enabled=true",
        "docgen.actuator.scrape-username=scrape-user",
        "docgen.actuator.scrape-password=scrape-secret",
        "management.endpoints.web.exposure.include=health,info,prometheus,metrics",
        "management.endpoint.prometheus.enabled=true",
        "management.metrics.export.prometheus.enabled=true"
})
class ActuatorScrapeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousPrometheusIsUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMetricsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMetricsChildPathIsUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validBasicCanScrapePrometheus() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scrape-user", "scrape-secret")))
                .andExpect(status().isOk());
    }

    @Test
    void wrongBasicIsUnauthorized() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scrape-user", "wrong-secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthzRemainsAnonymous() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());
    }

    @Test
    void readyzRemainsAnonymous() throws Exception {
        mockMvc.perform(get("/readyz"))
                .andExpect(status().isOk());
    }
}
