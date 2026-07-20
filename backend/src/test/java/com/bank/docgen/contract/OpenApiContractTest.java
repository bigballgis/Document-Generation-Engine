package com.bank.docgen.contract;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
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
            "getLifecycleAuditEvents",
            "reportRouteAccessDenied");

    private static final Set<String> REQUIRED_COLLABORATION_TIMEOUT_OPERATIONS = Set.of(
            "getCollaborationTimeoutConfig",
            "upsertCollaborationTimeoutConfig");

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
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSchemas()).containsKeys(
                "ErrorEnvelope",
                "Metadata"
        );

        List<String> operationIds = openAPI.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .map(operation -> operation.getOperationId())
                .toList();

        assertThat(operationIds).containsAll(REQUIRED_RUNTIME_OPERATIONS);
        assertThat(operationIds).containsAll(REQUIRED_ADMIN_AUDIT_OPERATIONS);
        assertThat(operationIds).containsAll(REQUIRED_COLLABORATION_TIMEOUT_OPERATIONS);
    }

    /**
     * BDD-CE-C01-C02-DOC-001: the OpenAPI {@code Context}, {@code GenerateRequest} and
     * {@code BatchGenerateRequest} schemas must declare the contract that the runtime DTOs bind,
     * including the optional {@code context} whitelist, {@code additionalProperties: false} and
     * the batch {@code originalBatchId} pattern field.
     */
    @Test
    void openapiV1DeclaresRuntimeContextAndStrictRequestSchemas() {
        Path contractPath = Path.of("..", "docs", "api", "openapi-v1.yaml").normalize();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readLocation(
                contractPath.toUri().toString(),
                null,
                options).getOpenAPI();

        Schema<?> context = openAPI.getComponents().getSchemas().get("Context");
        assertThat(context).isNotNull();
        assertAdditionalPropertiesForbidden(context);
        assertThat(context.getProperties()).containsOnlyKeys(
                "sourceSystem", "channel", "businessRequestId",
                "upstreamTraceId", "scenario", "locale",
                "jurisdiction", "product", "legalEntityCode");

        Schema<?> generateRequest = openAPI.getComponents().getSchemas().get("GenerateRequest");
        assertThat(generateRequest).isNotNull();
        assertAdditionalPropertiesForbidden(generateRequest);
        assertThat(generateRequest.getProperties())
                .containsKeys("output", "variables", "encryption", "requestId",
                        "idempotencyKey", "context");

        Schema<?> batchRequest = openAPI.getComponents().getSchemas().get("BatchGenerateRequest");
        assertThat(batchRequest).isNotNull();
        assertAdditionalPropertiesForbidden(batchRequest);
        assertThat(batchRequest.getProperties())
                .containsKeys("output", "encryption", "items", "requestId",
                        "idempotencyKey", "originalBatchId", "context");

        Schema<?> batchItem = openAPI.getComponents().getSchemas().get("BatchGenerateItem");
        assertThat(batchItem).isNotNull();
        assertAdditionalPropertiesForbidden(batchItem);
        assertThat(batchItem.getProperties())
                .containsOnlyKeys("itemId", "variables", "output", "encryption");

        // Variables stays an open map (additionalProperties: true) — CE-C02 must not tighten it.
        Schema<?> variables = openAPI.getComponents().getSchemas().get("Variables");
        assertThat(variables).isNotNull();
        assertThat(variables.getAdditionalProperties()).isNotEqualTo(Boolean.FALSE);

        // FieldError must enumerate UNKNOWN_FIELD (CE-C02) and INVALID_TYPE (CE-C01-005).
        Schema<?> fieldError = openAPI.getComponents().getSchemas().get("FieldError");
        assertThat(fieldError).isNotNull();
        Schema<?> reasonSchema = fieldError.getProperties().get("reason");
        assertThat(reasonSchema.getEnum())
                .extracting(Object::toString)
                .contains("UNKNOWN_FIELD", "INVALID_TYPE");
    }

    /**
     * OpenAPI 3.1 parsers may surface {@code additionalProperties: false} as {@link Boolean#FALSE}
     * or as an empty {@link Schema} sentinel — both mean "forbidden".
     */
    private static void assertAdditionalPropertiesForbidden(Schema<?> schema) {
        Object additional = schema.getAdditionalProperties();
        if (additional instanceof Boolean flag) {
            assertThat(flag).isFalse();
            return;
        }
        assertThat(additional).isInstanceOf(Schema.class);
        assertThat(additional).isNotEqualTo(Boolean.TRUE);
    }
}
