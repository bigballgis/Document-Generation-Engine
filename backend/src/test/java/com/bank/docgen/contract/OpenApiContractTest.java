package com.bank.docgen.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

    private static final Set<String> REQUIRED_RUNTIME_OPERATIONS = Set.of(
            "getTemplateApiContract",
            "listCallableVersions",
            "generateDocumentByVersion",
            "generateDocumentByDefaultRoute",
            "batchGenerateByVersion",
            "batchGenerateByDefaultRoute",
            "getAsyncTask",
            "cancelAsyncTask",
            "downloadDocument");

    private static final Set<String> REQUIRED_ADMIN_AUDIT_OPERATIONS = Set.of(
            "getManagementAuditEvents",
            "exportManagementAuditEvents",
            "getLifecycleAuditEvents");

    @Test
    void openapiV1ParsesAndContainsRuntimeOperations() {
        Path contractPath = Path.of("..", "docs", "api", "openapi-v1.yaml").normalize();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readLocation(
                contractPath.toUri().toString(),
                null,
                options).getOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getPaths()).isNotNull();

        List<String> operationIds = openAPI.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .map(operation -> operation.getOperationId())
                .toList();

        assertThat(operationIds).containsAll(REQUIRED_RUNTIME_OPERATIONS);
        assertThat(operationIds).containsAll(REQUIRED_ADMIN_AUDIT_OPERATIONS);
    }
}
