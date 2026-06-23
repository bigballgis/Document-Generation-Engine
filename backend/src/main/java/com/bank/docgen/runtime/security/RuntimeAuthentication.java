package com.bank.docgen.runtime.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class RuntimeAuthentication extends AbstractAuthenticationToken {

    private final RuntimeSessionClaims claims;

    public RuntimeAuthentication(RuntimeSessionClaims claims) {
        super(java.util.List.of());
        this.claims = claims;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return claims;
    }

    public RuntimeSessionClaims claims() {
        return claims;
    }
}
