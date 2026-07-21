package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateExportAccessServiceTest {

    private TemplateExportAccessService support;

    @BeforeEach
    void setUp() {
        support = new TemplateExportAccessService(new GroupAccessService());
    }

    @Test
    void globalAdmin_canExportAnyGroupTemplate() {
        TemplateEntity template = template("RETAIL", "10000003");
        ManagementSessionClaims globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));

        assertThatCode(() -> support.assertCanExport(template, globalAdmin)).doesNotThrowAnyException();
    }

    @Test
    void groupAdmin_canExportScopedGroupTemplate() {
        TemplateEntity template = template("RETAIL", "10000003");
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));

        assertThatCode(() -> support.assertCanExport(template, groupAdmin)).doesNotThrowAnyException();
    }

    @Test
    void groupAdmin_deniedForOtherGroup() {
        TemplateEntity template = template("CORP", "10000003");
        ManagementSessionClaims groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));

        assertThatThrownBy(() -> support.assertCanExport(template, groupAdmin))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void templateAuthor_canExportOwnTemplate() {
        TemplateEntity template = template("RETAIL", "10000003");
        ManagementSessionClaims author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));

        assertThatCode(() -> support.assertCanExport(template, author)).doesNotThrowAnyException();
    }

    @Test
    void templateAuthor_deniedForOthersTemplate() {
        TemplateEntity template = template("RETAIL", "10000004");
        ManagementSessionClaims author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));

        assertThatThrownBy(() -> support.assertCanExport(template, author))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void tester_denied() {
        TemplateEntity template = template("RETAIL", "10000003");
        ManagementSessionClaims tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));

        assertThatThrownBy(() -> support.assertCanExport(template, tester))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void assertCanImportForGroup_deniesTester() {
        ManagementSessionClaims tester = session("10000006", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));

        assertThatThrownBy(() -> support.assertCanImportForGroup("RETAIL", tester))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void assertCanExport_matchesCanExport() {
        TemplateEntity own = template("RETAIL", "10000003");
        TemplateEntity other = template("RETAIL", "10000004");
        ManagementSessionClaims author = session("10000003", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));

        assertThat(support.canExport(own, author)).isTrue();
        assertThatCode(() -> support.assertCanExport(own, author)).doesNotThrowAnyException();
        assertThat(support.canExport(other, author)).isFalse();
        assertThatThrownBy(() -> support.assertCanExport(other, author))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    private static TemplateEntity template(String groupCode, String createdBy) {
        return new TemplateEntity(
                UUID.randomUUID(),
                "TPL-EXPORT-TEST",
                groupCode,
                "Export Test",
                "Desc",
                UUID.randomUUID(),
                createdBy
        );
    }

    private static ManagementSessionClaims session(String username, List<String> roles) {
        return session(username, roles, List.of("RETAIL"));
    }

    private static ManagementSessionClaims session(
            String username,
            List<String> roles,
            List<String> authorizedGroupCodes
    ) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                authorizedGroupCodes,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
