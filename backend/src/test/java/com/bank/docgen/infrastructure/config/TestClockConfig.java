package com.bank.docgen.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
