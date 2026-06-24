package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.DemoCatalogSeeder;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "docgen.demo-catalog.seed-enabled=true")
class DemoCatalogSeederIntegrationTest {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private MasterDocumentRepository masterDocumentRepository;

    @Test
    void seedsApprovedDemoMasterAndDraftTemplate() {
        var template = templateRepository.findByExternalIdAndDeletedAtIsNull(DemoCatalogSeeder.DEMO_TEMPLATE_EXTERNAL_ID);
        assertThat(template).isPresent();
        assertThat(template.get().getName()).isEqualTo(DemoCatalogSeeder.DEMO_TEMPLATE_NAME);
        assertThat(template.get().getGroupCode()).isEqualTo(DemoCatalogSeeder.DEMO_GROUP_CODE);

        var masters = masterDocumentRepository.findByDeletedAtIsNullAndGroupCodeInOrderByUpdatedAtDesc(
                List.of(DemoCatalogSeeder.DEMO_GROUP_CODE)
        );
        assertThat(masters).anyMatch(
                master ->
                        DemoCatalogSeeder.DEMO_MASTER_NAME.equals(master.getName())
                                && master.getStatus() == MasterDocumentStatus.APPROVED
        );
    }
}
