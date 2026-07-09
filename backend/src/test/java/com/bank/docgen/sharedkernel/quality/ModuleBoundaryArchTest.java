package com.bank.docgen.sharedkernel.quality;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.bank.docgen.template.service.TemplateValidationException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Module boundary enforcement for CODE-QUALITY CQ-01.
 * Phase 1 rules are active; Phase 2 rules unlock after Port wiring (CQ-01B).
 */
class ModuleBoundaryArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bank.docgen");
    }

    @Test
    void sharedKernelDocumentMustNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.sharedkernel.document..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void sharedKernelDocumentMustNotDependOnBusinessModules() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.sharedkernel.document..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.bank.docgen.template..",
                        "com.bank.docgen.rendering..",
                        "com.bank.docgen.authoring..",
                        "com.bank.docgen.runtime..",
                        "com.bank.docgen.apimgmt..",
                        "com.bank.docgen.audit..",
                        "com.bank.docgen.master..",
                        "com.bank.docgen.contentmodule..",
                        "com.bank.docgen.collaboration.."
                )
                .check(classes);
    }

    @Test
    void fidelityWarningCodeLivesInSharedKernelDocument() {
        classes()
                .that().haveSimpleName("FidelityWarningCode")
                .should().resideInAPackage("com.bank.docgen.sharedkernel.document.fidelity")
                .check(classes);
    }

    @Test
    void masterStyleCatalogLivesInSharedKernelDocumentStyle() {
        classes()
                .that().haveSimpleName("MasterStyleCatalog")
                .should().resideInAPackage("com.bank.docgen.sharedkernel.document.style")
                .check(classes);
    }

    @Test
    void renderProfileLivesInSharedKernelDocument() {
        classes()
                .that().haveSimpleName("RenderProfile")
                .should().resideInAPackage("com.bank.docgen.sharedkernel.document")
                .check(classes);
    }

    @Test
    void conditionExpressionEvaluatorLivesInSharedKernelDocumentExpression() {
        classes()
                .that().haveSimpleName("ConditionExpressionEvaluator")
                .should().resideInAPackage("com.bank.docgen.sharedkernel.document.expression")
                .check(classes);
    }

    @Test
    void authoringMustNotDependOnRenderingDomain() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.authoring..")
                .should().dependOnClassesThat().resideInAPackage("com.bank.docgen.rendering.domain..")
                .check(classes);
    }

    @Test
    void templateMustNotDependOnRenderingPersistence() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.template..")
                .should().dependOnClassesThat().resideInAPackage("com.bank.docgen.rendering.persistence..")
                .check(classes);
    }

    @Test
    void renderingMustNotUseTemplateValidationException() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.rendering..")
                .should().dependOnClassesThat().areAssignableTo(TemplateValidationException.class)
                .check(classes);
    }

    @Test
    void renderingMustNotDependOnTemplateService() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.rendering..")
                .should().dependOnClassesThat().resideInAPackage("com.bank.docgen.template.service..")
                .check(classes);
    }

    @Test
    void renderingKernelMustNotDependOnAuthoring() {
        noClasses()
                .that().resideInAPackage("com.bank.docgen.rendering")
                .and().resideOutsideOfPackage("com.bank.docgen.rendering.service..")
                .and().resideOutsideOfPackage("com.bank.docgen.rendering.web..")
                .and().resideOutsideOfPackage("com.bank.docgen.rendering.persistence..")
                .and().resideOutsideOfPackage("com.bank.docgen.rendering.listener..")
                .and().resideOutsideOfPackage("com.bank.docgen.rendering.scheduler..")
                .should().dependOnClassesThat().resideInAPackage("com.bank.docgen.authoring..")
                .check(classes);
    }
}
