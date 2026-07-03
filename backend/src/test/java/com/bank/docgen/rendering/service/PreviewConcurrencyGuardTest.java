package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PreviewConcurrencyGuardTest {

    @Test
    void tryAcquire_belowLimit_returnsTrue() {
        PreviewConcurrencyGuard guard = new PreviewConcurrencyGuard(3);

        assertThat(guard.tryAcquire()).isTrue();
        assertThat(guard.tryAcquire()).isTrue();
        assertThat(guard.tryAcquire()).isTrue();
        assertThat(guard.getActiveCount()).isEqualTo(3);
    }

    @Test
    void tryAcquire_atLimit_returnsFalse() {
        PreviewConcurrencyGuard guard = new PreviewConcurrencyGuard(2);
        guard.tryAcquire();
        guard.tryAcquire();

        assertThat(guard.tryAcquire()).isFalse();
        assertThat(guard.getActiveCount()).isEqualTo(2);
    }

    @Test
    void release_decrementsCount() {
        PreviewConcurrencyGuard guard = new PreviewConcurrencyGuard(2);
        guard.tryAcquire();
        guard.tryAcquire();

        guard.release();

        assertThat(guard.getActiveCount()).isEqualTo(1);
        assertThat(guard.tryAcquire()).isTrue();
    }

    @Test
    void release_afterAtLimit_allowsNewAcquire() {
        PreviewConcurrencyGuard guard = new PreviewConcurrencyGuard(1);
        guard.tryAcquire();
        assertThat(guard.tryAcquire()).isFalse();

        guard.release();

        assertThat(guard.tryAcquire()).isTrue();
    }

    @Test
    void getMaxConcurrent_returnsConfiguredValue() {
        PreviewConcurrencyGuard guard = new PreviewConcurrencyGuard(5);
        assertThat(guard.getMaxConcurrent()).isEqualTo(5);
    }
}
