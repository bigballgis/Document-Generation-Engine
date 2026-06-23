package com.bank.docgen.sharedkernel.security;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PasswordHashGeneratorTest {

    @Test
    @Disabled("Manual helper to print Argon2 hash for Flyway seed data")
    void printSeedPasswordHash() {
        PasswordHashService service = new PasswordHashService();
        System.out.println(service.hash("ChangeMe123!"));
    }
}
