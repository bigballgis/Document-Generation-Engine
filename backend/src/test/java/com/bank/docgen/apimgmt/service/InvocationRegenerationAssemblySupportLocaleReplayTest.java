package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.sharedkernel.document.compute.VariableComputeEngine;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VariableComputeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * IBL-A6 — regenerate assembly must replay retained {@code contextSummary.locale}
 * into {@code VariableComputeService.applyCompute} (BDD-IBL-A6-001…008, 006).
 */
@ExtendWith(MockitoExtension.class)
class InvocationRegenerationAssemblySupportLocaleReplayTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MASTER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID REVISION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final VariableComputeEngine ENGINE = VariableComputeEngine.INSTANCE;

    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private MasterRevisionLineRepository masterRevisionLineRepository;
    @Mock private AnchorBindingRepository anchorBindingRepository;
    @Mock private ObjectStoragePort objectStoragePort;
    @Mock private DocxAssembler docxAssembler;
    @Mock private DocumentArtifactPipeline documentArtifactPipeline;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private RenderProfileService renderProfileService;
    @Mock private VariableComputeService variableComputeService;

    private InvocationRegenerationAssemblySupport support;
    private TemplateEntity template;
    private byte[] masterDocx;
    private String masterHash;

    @BeforeEach
    void setUp() throws Exception {
        support = new InvocationRegenerationAssemblySupport(
                templateVersionRepository,
                masterRevisionLineRepository,
                anchorBindingRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                contentModuleReferenceService,
                renderProfileService,
                variableComputeService,
                new ObjectMapper()
        );
        template = new TemplateEntity(TEMPLATE_ID, "TPL-001", "GRP-A", "Sample", null, MASTER_ID, "10000001");
        masterDocx = minimalDocx("regenerate-body");
        masterHash = sha256Hex(masterDocx);

        lenient().when(variableComputeService.applyCompute(any(), any(), any())).thenAnswer(invocation -> {
            Map<String, Object> input = invocation.getArgument(1);
            return input == null ? Map.of() : new LinkedHashMap<>(input);
        });
    }

    @Test
    void bddIblA6_001_retainedEnUsLocalePassedToApplyCompute() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.SINGLE,
                """
                {"variables":{"principal":1234.56},"contextSummary":{"locale":"en-US"}}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), eq("en-US"));
    }

    @Test
    void bddIblA6_002_003_004_localeSensitiveComputeFollowsRetainedEnUs() throws Exception {
        when(variableComputeService.applyCompute(any(), any(), any())).thenAnswer(invocation -> {
            Map<String, Object> input = invocation.getArgument(1);
            String localeTag = invocation.getArgument(2);
            Map<String, Object> bindings = input == null ? Map.of() : new LinkedHashMap<>(input);
            Map<String, Object> resolved = new LinkedHashMap<>(bindings);
            // Binary ISO FORMAT_AMOUNT keeps EUR identity across many locales; unary amount +
            // FORMAT_DATE + binary SPELL_AMOUNT(en,USD) are observably locale-sensitive.
            resolved.put("formattedAmount", ENGINE.evaluateSingle(
                    "formattedAmount",
                    "FORMAT_AMOUNT(${principal})",
                    bindings,
                    localeTag
            ));
            resolved.put("formattedAmountEur", ENGINE.evaluateSingle(
                    "formattedAmountEur",
                    "FORMAT_AMOUNT(${principal}, 'EUR')",
                    bindings,
                    localeTag
            ));
            resolved.put("formattedDate", ENGINE.evaluateSingle(
                    "formattedDate",
                    "FORMAT_DATE(${signDate})",
                    bindings,
                    localeTag
            ));
            resolved.put("spelledAmount", ENGINE.evaluateSingle(
                    "spelledAmount",
                    "SPELL_AMOUNT(${principal}, 'USD')",
                    bindings,
                    localeTag
            ));
            return resolved;
        });

        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.SINGLE,
                """
                {"variables":{"principal":1234.56,"signDate":"2024-01-15"},\
                "contextSummary":{"locale":"en-US"}}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), eq("en-US"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> resolvedCaptor = ArgumentCaptor.forClass(Map.class);
        verify(docxAssembler).assembleStructured(any(), any(), resolvedCaptor.capture(), any());
        Map<String, Object> resolved = resolvedCaptor.getValue();

        Map<String, Object> bindings = Map.of(
                "principal", new BigDecimal("1234.56"),
                "signDate", "2024-01-15"
        );
        String amountEn = String.valueOf(ENGINE.evaluateSingle(
                "formattedAmount", "FORMAT_AMOUNT(${principal})", bindings, "en-US"));
        String amountZh = String.valueOf(ENGINE.evaluateSingle(
                "formattedAmount", "FORMAT_AMOUNT(${principal})", bindings, null));
        String amountEurEn = String.valueOf(ENGINE.evaluateSingle(
                "formattedAmountEur", "FORMAT_AMOUNT(${principal}, 'EUR')", bindings, "en-US"));
        String dateEn = String.valueOf(ENGINE.evaluateSingle(
                "formattedDate", "FORMAT_DATE(${signDate})", bindings, "en-US"));
        String dateZh = String.valueOf(ENGINE.evaluateSingle(
                "formattedDate", "FORMAT_DATE(${signDate})", bindings, null));
        String spellEn = String.valueOf(ENGINE.evaluateSingle(
                "spelledAmount", "SPELL_AMOUNT(${principal}, 'USD')", bindings, "en-US"));

        assertThat(amountEn).isNotEqualTo(amountZh);
        assertThat(dateEn).isNotEqualTo(dateZh);
        assertThat(resolved.get("formattedAmount")).isEqualTo(amountEn);
        assertThat(resolved.get("formattedAmountEur")).isEqualTo(amountEurEn);
        assertThat(resolved.get("formattedDate")).isEqualTo(dateEn);
        assertThat(resolved.get("spelledAmount")).isEqualTo(spellEn);
        assertThat(resolved.get("formattedAmount")).isNotEqualTo(amountZh);
        assertThat(resolved.get("formattedDate")).isNotEqualTo(dateZh);
        assertThat(String.valueOf(resolved.get("spelledAmount"))).contains("USD");
        assertThat(String.valueOf(resolved.get("spelledAmount"))).doesNotContain("元", "角", "分", "整");
    }

    @Test
    void bddIblA6_005_missingLocalePassesNullToApplyCompute() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.SINGLE,
                """
                {"variables":{"principal":100}}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), isNull());
    }

    @Test
    void bddIblA6_005_blankLocalePassesNullToApplyCompute() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.SINGLE,
                """
                {"variables":{"principal":100},"contextSummary":{"locale":"  "}}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), isNull());
    }

    @Test
    void bddIblA6_006_regeneratedDocxStillContainsSpecimen() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.SINGLE,
                """
                {"variables":{"name":"Alice"},"contextSummary":{"locale":"en-US"}}
                """
        );
        stubSuccessfulDocxAssembly();

        InvocationRegenerationAssemblySupport.AssembledRegeneration result =
                support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        ArgumentCaptor<InputStream> putStream = ArgumentCaptor.forClass(InputStream.class);
        verify(objectStoragePort).put(anyString(), putStream.capture(), anyLong(), anyString());
        byte[] stored = putStream.getValue().readAllBytes();
        assertThat(anyZipPartContains(stored, "word/header", "SPECIMEN")
                || anyZipPartContains(stored, "word/footer", "SPECIMEN")).isTrue();
        assertThat(result.contentType()).contains("wordprocessingml");
    }

    @Test
    void bddIblA6_007_batchItemWithRetainedLocaleReplaysEnUs() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.BATCH_ITEM,
                """
                {"variables":{"principal":1234.56},"contextSummary":{"locale":"en-US"}}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), eq("en-US"));
    }

    @Test
    void bddIblA6_008_historicalBatchItemWithoutLocaleDoesNotInventLocale() throws Exception {
        ApiInvocationRecordEntity invocation = invocationWithParameters(
                InvocationKind.BATCH_ITEM,
                """
                {"variables":{"principal":1234.56},"itemId":"item-1"}
                """
        );
        stubSuccessfulDocxAssembly();

        support.assembleSpecimen(template, invocation, "DOCX", UUID.randomUUID());

        verify(variableComputeService).applyCompute(eq(VERSION_ID), any(), isNull());
    }

    private void stubSuccessfulDocxAssembly() throws Exception {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setReleaseVersion("1.0.0");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setMasterRevisionId(REVISION_ID);
        MasterRevisionLineEntity revision = new MasterRevisionLineEntity(
                REVISION_ID,
                MASTER_ID,
                "masters/pinned.docx",
                "master.docx",
                1,
                MasterDocumentStatus.APPROVED,
                1,
                true,
                "change",
                "10000001"
        );
        when(templateVersionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(REVISION_ID, MASTER_ID))
                .thenReturn(Optional.of(revision));
        when(objectStoragePort.get("masters/pinned.docx")).thenReturn(new ByteArrayInputStream(masterDocx));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(VERSION_ID)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(Map.of());
        when(docxAssembler.assembleStructured(any(), any(), any(), any())).thenReturn(masterDocx);
        when(renderProfileService.resolveEffectiveProfile(any(), any())).thenReturn(renderProfile());
        doAnswer(inv -> null).when(objectStoragePort).put(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    private ApiInvocationRecordEntity invocationWithParameters(InvocationKind kind, String parametersJson) {
        Instant now = Instant.now();
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                kind == InvocationKind.BATCH_ITEM ? "INV-ITEM01" : "INV-OK01",
                kind,
                InvocationStatus.SUCCEEDED,
                "dev",
                TEMPLATE_ID,
                "TPL-001",
                UUID.randomUUID(),
                "svc-account",
                "req-1",
                "idem-1",
                "EXPLICIT_VERSION",
                "1.0.0",
                "1.0.0",
                "DOCX",
                "SYNC_STREAM",
                "SUCCESS",
                12L,
                parametersJson,
                "DOC-1",
                "generated/doc-1/output.docx",
                true,
                now.plusSeconds(3600),
                now.plusSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "audit-1",
                kind != InvocationKind.SINGLE,
                now,
                now
        );
        entity.applyReleaseBundleFingerprint(VERSION_ID, masterHash);
        return entity;
    }

    private static RenderProfile renderProfile() {
        return new RenderProfile(
                "rp-v1",
                "MASTER_CATALOG_LOCKED",
                "CONTROLLED_MULTILEVEL",
                "REPEAT_HEADER",
                "PROPORTIONAL_FIT",
                "SEMANTIC_FIDELITY",
                "BLOCKERS_PREVENT_PUBLISH",
                false,
                PdfArchivalProfile.NONE
        );
    }

    private static byte[] minimalDocx(String body) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(body);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static boolean anyZipPartContains(byte[] docxBytes, String namePrefix, String substring)
            throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith(namePrefix) && name.endsWith(".xml")) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    if (xml.contains(substring)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
