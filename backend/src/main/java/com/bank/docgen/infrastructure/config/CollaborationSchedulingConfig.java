package com.bank.docgen.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("!test")
public class CollaborationSchedulingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
