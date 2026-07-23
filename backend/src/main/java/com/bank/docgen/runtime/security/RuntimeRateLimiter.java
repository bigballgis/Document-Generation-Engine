package com.bank.docgen.runtime.security;

/**
 * Runtime API rate-limit gateway — process-local or Redis-coordinated.
 */
public interface RuntimeRateLimiter {

    boolean enabled();

    RateLimitDecision tryConsumeKey(String key);

    default RateLimitDecision tryConsume(String credentialExternalId, String accessAccount) {
        return tryConsumeKey(credentialExternalId + ":" + accessAccount);
    }
}
