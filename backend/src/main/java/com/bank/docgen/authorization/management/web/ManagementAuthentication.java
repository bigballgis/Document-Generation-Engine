package com.bank.docgen.authorization.management.web;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ManagementAuthentication implements Authentication {

    private final ManagementSessionClaims session;
    private boolean authenticated = true;

    public ManagementAuthentication(ManagementSessionClaims session) {
        this.session = session;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return session.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return session;
    }

    @Override
    public Object getPrincipal() {
        return session;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return session.username();
    }

    public List<String> visibleRoutes() {
        return session.visibleRoutes();
    }
}
