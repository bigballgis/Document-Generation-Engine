package com.bank.docgen.sharedkernel.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

/**
 * Ops acceptance pin for Task #51 / boot-4-1-upgrade — platform baseline, not product BDD.
 * ADR-0028 amended: Java 25 + Spring Boot 4.1.0.
 */
class SpringBootPlatformBaselineTest {

    @Test
    void javaRuntimeIsTwentyFive() {
        assertEquals(
                25,
                Runtime.version().feature(),
                () -> "Expected Java 25 runtime, was: " + Runtime.version()
        );
    }

    @Test
    void springBootRuntimeIsFourOneLine() {
        String version = SpringBootVersion.getVersion();
        assertTrue(
                version != null && version.startsWith("4.1."),
                () -> "Expected Spring Boot 4.1.x runtime, was: " + version
        );
        assertEquals("4.1.0", version);
    }
}
