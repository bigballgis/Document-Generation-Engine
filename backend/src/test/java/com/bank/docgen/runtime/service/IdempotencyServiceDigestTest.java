package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LR-B7 (OPT-E9): digest failure must be a hard error — never a silently weakened
 * idempotency key built from the raw payload.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceDigestTest {

    @Mock
    private GenerationIdempotencyRepository repository;

    @Mock
    private IdempotencyCachePort cachePort;

    @Test
    void hashRequestReturnsSha256HexOnHappyPath() {
        IdempotencyService service = new IdempotencyService(repository, cachePort);

        String hash = service.hashRequest("abc");

        assertThat(hash)
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void digestFailureThrowsHardErrorAndNeverFallsBackToRawPayload() {
        IdempotencyService service = new IdempotencyService(repository, cachePort) {
            @Override
            protected MessageDigest newDigest() throws NoSuchAlgorithmException {
                throw new NoSuchAlgorithmException("SHA-256 unavailable");
            }
        };

        assertThatThrownBy(() -> service.hashRequest("sensitive-payload"))
                .isInstanceOf(IdempotencyDigestException.class)
                .hasMessageNotContaining("sensitive-payload");
    }
}
