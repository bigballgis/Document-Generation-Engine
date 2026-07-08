package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementUserDisplayServiceTest {

    @Mock
    private ManagementUserRepository managementUserRepository;

    private ManagementUserDisplayService service;

    @BeforeEach
    void setUp() {
        service = new ManagementUserDisplayService(managementUserRepository);
    }

    @Test
    void lookupDisplayNames_formatsKnownUsers() {
        when(managementUserRepository.findByUsernameInAndDeletedAtIsNull(Set.of("10000001", "10000002")))
                .thenReturn(List.of(
                        user("10000001", "Global Admin"),
                        user("10000002", "Group Admin")
                ));

        Map<String, String> displayNames = service.lookupDisplayNames(Set.of("10000001", "10000002"));

        assertThat(displayNames).containsEntry("10000001", "Global Admin (10000001)");
        assertThat(displayNames).containsEntry("10000002", "Group Admin (10000002)");
    }

    @Test
    void lookupDisplayNames_fallsBackToUsernameWhenUserMissing() {
        when(managementUserRepository.findByUsernameInAndDeletedAtIsNull(Set.of("10000099")))
                .thenReturn(List.of());

        Map<String, String> displayNames = service.lookupDisplayNames(Set.of("10000099"));

        assertThat(displayNames).containsEntry("10000099", "10000099");
    }

    @Test
    void lookupDisplayNames_usesUsernameWhenDisplayNameBlank() {
        when(managementUserRepository.findByUsernameInAndDeletedAtIsNull(Set.of("10000003")))
                .thenReturn(List.of(user("10000003", "   ")));

        Map<String, String> displayNames = service.lookupDisplayNames(Set.of("10000003"));

        assertThat(displayNames).containsEntry("10000003", "10000003");
    }

    @Test
    void lookupDisplayNames_returnsEmptyMapForEmptyInput() {
        assertThat(service.lookupDisplayNames(Set.of())).isEmpty();
        assertThat(service.lookupDisplayNames(null)).isEmpty();
    }

    private static ManagementUserEntity user(String username, String displayName) {
        return new ManagementUserEntity(
                UUID.randomUUID(),
                username,
                displayName,
                username + "@example.com",
                "hash",
                AuthSource.LOCAL,
                Set.of(ManagementRole.GLOBAL_ADMIN),
                Set.of()
        );
    }
}
