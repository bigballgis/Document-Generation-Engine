package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AuthorWordPageCountView;
import com.bank.docgen.template.api.UpdateAuthorWordPageCountRequest;
import com.bank.docgen.template.mapping.TemplateMasterPinMapper;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-PRR-C01-008 — authorWordPageCount management API persistence.
 */
@ExtendWith(MockitoExtension.class)
class AuthorWordPageCountServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private TemplateContentModuleReferenceRepository contentModuleReferenceRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateViewMapper templateViewMapper;
    @Mock
    private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;
    @Mock
    private TemplateMasterPinMapper templateMasterPinMapper;

    private TemplateVersionLineService service;
    private UUID templateId;
    private UUID versionId;
    private TemplateVersionEntity version;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new TemplateVersionLineService(
                templateService,
                templateRepository,
                templateVersionRepository,
                templateCurrentVersionResolver,
                variableSchemaRepository,
                anchorBindingRepository,
                contentModuleReferenceRepository,
                lifecycleRecordRepository,
                apiPolicyRepository,
                templateViewMapper,
                approvalSubStateResolver,
                groupAccessService,
                messageResolver,
                managementUserDisplayService,
                templateMasterPinMapper
        );
        templateId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        version = new TemplateVersionEntity(versionId, templateId, "10000001");
        session = new ManagementSessionClaims(
                "10000001",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"),
                List.of("RETAIL"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void setAndReadAuthorWordPageCount_persists() {
        TemplateEntity template = new TemplateEntity(
                templateId, "TPL-1", "RETAIL", "Demo", null, UUID.randomUUID(), "10000001");
        when(templateService.requireWritableTemplate(templateId, session)).thenReturn(template);
        when(templateService.requireReadableTemplate(templateId, session)).thenReturn(template);
        when(templateCurrentVersionResolver.requireInFlightDevVersion(templateId)).thenReturn(version);
        when(templateCurrentVersionResolver.isInFlight(version)).thenReturn(true);
        when(templateVersionRepository.save(version)).thenReturn(version);

        AuthorWordPageCountView updated = service.updateAuthorWordPageCount(
                templateId,
                new UpdateAuthorWordPageCountRequest(6),
                session
        );

        assertThat(updated.authorWordPageCount()).isEqualTo(6);
        assertThat(version.getAuthorWordPageCount()).isEqualTo(6);
        verify(templateVersionRepository).save(version);

        AuthorWordPageCountView read = service.getAuthorWordPageCount(templateId, session);
        assertThat(read.authorWordPageCount()).isEqualTo(6);
        assertThat(read.devVersionId()).isEqualTo(versionId.toString());
    }

    @Test
    void clearAuthorWordPageCount_allowsSkipSemantics() {
        TemplateEntity template = new TemplateEntity(
                templateId, "TPL-1", "RETAIL", "Demo", null, UUID.randomUUID(), "10000001");
        version.setAuthorWordPageCount(6);
        when(templateService.requireWritableTemplate(templateId, session)).thenReturn(template);
        when(templateCurrentVersionResolver.requireInFlightDevVersion(templateId)).thenReturn(version);
        when(templateCurrentVersionResolver.isInFlight(version)).thenReturn(true);
        when(templateVersionRepository.save(version)).thenReturn(version);

        AuthorWordPageCountView cleared = service.updateAuthorWordPageCount(
                templateId,
                new UpdateAuthorWordPageCountRequest(null),
                session
        );

        assertThat(cleared.authorWordPageCount()).isNull();
        assertThat(version.getAuthorWordPageCount()).isNull();
    }
}
