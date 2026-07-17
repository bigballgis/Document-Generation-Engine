package com.bank.docgen.apimgmt.service;

import java.util.Collections;
import java.util.List;

/**
 * Consumer-side AD Group intersection authorization (BDD-PRR-B02-LDAP-SPI-002 / SPI-C3).
 *
 * <p>Not part of the {@link AdGroupResolver} SPI — callers resolve groups via the interface, then
 * apply policy intersection here (or inline equivalent).
 */
public final class AdGroupAuthorization {

    private AdGroupAuthorization() {
    }

    public static boolean isAuthorized(
            AdGroupResolver resolver,
            String accessAccount,
            List<String> allowedAdGroups
    ) {
        if (resolver == null) {
            return false;
        }
        return isAuthorized(resolver.resolveGroups(accessAccount), allowedAdGroups);
    }

    public static boolean isAuthorized(List<String> callerGroups, List<String> allowedAdGroups) {
        if (allowedAdGroups == null || allowedAdGroups.isEmpty()) {
            return false;
        }
        if (callerGroups == null || callerGroups.isEmpty()) {
            return false;
        }
        return !Collections.disjoint(callerGroups, allowedAdGroups);
    }
}
