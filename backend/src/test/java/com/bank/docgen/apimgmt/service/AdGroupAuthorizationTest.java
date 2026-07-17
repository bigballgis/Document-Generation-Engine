package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-B02-LDAP-SPI-002 / SPI-C3 — intersection lives on consumer helper, not SPI.
 */
class AdGroupAuthorizationTest {

    @Test
    void intersectionHitWhenCallerSharesAllowedGroup() {
        AdGroupResolver resolver = mock(AdGroupResolver.class);
        when(resolver.resolveGroups("svc-caller")).thenReturn(List.of("RETAIL_API", "CORP_API"));

        assertThat(AdGroupAuthorization.isAuthorized(resolver, "svc-caller", List.of("RETAIL_API")))
                .isTrue();
    }

    @Test
    void intersectionMissWhenCallerHasNoOverlap() {
        AdGroupResolver resolver = mock(AdGroupResolver.class);
        when(resolver.resolveGroups("svc-caller")).thenReturn(List.of("CORP_API"));

        assertThat(AdGroupAuthorization.isAuthorized(resolver, "svc-caller", List.of("RETAIL_API")))
                .isFalse();
    }

    @Test
    void denyWhenAllowedGroupsEmptyOrNull() {
        AdGroupResolver resolver = mock(AdGroupResolver.class);

        assertThat(AdGroupAuthorization.isAuthorized(resolver, "svc-caller", List.of())).isFalse();
        assertThat(AdGroupAuthorization.isAuthorized(resolver, "svc-caller", null)).isFalse();
    }

    @Test
    void denyWhenResolveGroupsEmpty() {
        AdGroupResolver resolver = mock(AdGroupResolver.class);
        when(resolver.resolveGroups("unknown")).thenReturn(List.of());

        assertThat(AdGroupAuthorization.isAuthorized(resolver, "unknown", List.of("RETAIL_API")))
                .isFalse();
    }

    @Test
    void worksWithNonConfigResolverImplementation() {
        AdGroupResolver resolver = accessAccount -> {
            if ("svc-caller".equals(accessAccount)) {
                return List.of("RETAIL_API");
            }
            return List.of();
        };

        assertThat(AdGroupAuthorization.isAuthorized(resolver, "svc-caller", List.of("RETAIL_API")))
                .isTrue();
        assertThat(AdGroupAuthorization.isAuthorized(resolver, "other", List.of("RETAIL_API")))
                .isFalse();
    }
}
