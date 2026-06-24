package com.bank.docgen.authorization.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.stereotype.Service;

class ManagementAuthorizationContractTest {

    private static final Path BACKEND_MAIN = Path.of("src", "main", "java", "com", "bank", "docgen");

    private static final Set<String> MANAGEMENT_AUTHORIZATION_SERVICES = Set.of(
            "com.bank.docgen.template.service.TemplateService",
            "com.bank.docgen.template.service.TemplateLifecycleService",
            "com.bank.docgen.template.service.TemplateDeleteService",
            "com.bank.docgen.master.service.MasterDocumentService",
            "com.bank.docgen.apimgmt.service.ApiManagementService",
            "com.bank.docgen.apimgmt.service.ApiPolicyImpactPreviewService",
            "com.bank.docgen.audit.service.AuditQueryService"
    );

    private static final Set<String> MANAGEMENT_CONTROLLERS = Set.of(
            "com.bank.docgen.template.web.TemplateController",
            "com.bank.docgen.master.web.MasterDocumentController",
            "com.bank.docgen.apimgmt.web.ApiManagementController",
            "com.bank.docgen.audit.web.AuditController"
    );

    @ParameterizedTest
    @MethodSource("managementAuthorizationServices")
    void managementServicesDelegateToGroupAccessService(Class<?> serviceClass) {
        assertThat(serviceClass.getAnnotation(Service.class)).isNotNull();
        assertThat(hasGroupAccessServiceField(serviceClass))
                .as("%s should inject GroupAccessService for capability checks", serviceClass.getSimpleName())
                .isTrue();
    }

    @Test
    void managementControllersDoNotReferenceManagementRoute() throws IOException {
        for (String controllerName : MANAGEMENT_CONTROLLERS) {
            String relativePath = controllerName.replace("com.bank.docgen.", "").replace('.', '/')
                    + ".java";
            Path source = BACKEND_MAIN.resolve(relativePath);
            String content = Files.readString(source);
            assertThat(content)
                    .as("%s must not use ManagementRoute for API authorization", controllerName)
                    .doesNotContain("ManagementRoute");
        }
    }

    @Test
    void groupAccessServiceExposesManagementCapabilities() {
        assertThat(GroupAccessService.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .contains(
                        "canManageMasters",
                        "canPublishTemplates",
                        "canManageApiPolicy",
                        "canReadAudit",
                        "canAccessGroup"
                );
    }

    private static Stream<Class<?>> managementAuthorizationServices() {
        return MANAGEMENT_AUTHORIZATION_SERVICES.stream().map(ManagementAuthorizationContractTest::loadClass);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Missing management authorization service: " + className, ex);
        }
    }

    private static boolean hasGroupAccessServiceField(Class<?> serviceClass) {
        return Stream.of(serviceClass.getDeclaredFields())
                .anyMatch(field -> GroupAccessService.class.equals(field.getType()));
    }
}
