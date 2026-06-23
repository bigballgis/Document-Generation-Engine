package com.bank.docgen.authorization.management.persistence;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "management_user")
public class ManagementUserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 8)
    private String username;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(nullable = false, length = 256)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_source", nullable = false, length = 32)
    private AuthSource authSource = AuthSource.LOCAL;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "management_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private Set<ManagementRole> roles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "management_user_group_scope", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "group_code", nullable = false, length = 64)
    private Set<String> authorizedGroupCodes = new LinkedHashSet<>();

    protected ManagementUserEntity() {
    }

    public ManagementUserEntity(
            UUID id,
            String username,
            String displayName,
            String email,
            String passwordHash,
            AuthSource authSource,
            Set<ManagementRole> roles,
            Set<String> authorizedGroupCodes
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authSource = authSource;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.roles = new LinkedHashSet<>(roles);
        this.authorizedGroupCodes = new LinkedHashSet<>(authorizedGroupCodes);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthSource getAuthSource() {
        return authSource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<ManagementRole> getRoles() {
        return Set.copyOf(roles);
    }

    public Set<String> getAuthorizedGroupCodes() {
        return Set.copyOf(authorizedGroupCodes);
    }
}
