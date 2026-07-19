package com.bank.docgen.rendering.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.rendering.service.PreviewGenerationException;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.ErrorEnvelope;
import com.bank.docgen.sharedkernel.api.ErrorEnvelopeFactory;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RenderingExceptionAdviceTest {

    @Mock
    private MessageResolver messageResolver;

    private RenderingExceptionAdvice advice;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        advice = new RenderingExceptionAdvice(
                new ErrorEnvelopeFactory(new TraceIdProvider(), messageResolver)
        );
        request = new MockHttpServletRequest("POST", "/api/management/v1/templates/1/previews");
    }

    @Test
    void objectStorageExceptionMapsToInternalErrorEnvelope() {
        when(messageResolver.resolve("api.error.storage.operationFailed"))
                .thenReturn("Object storage operation failed.");

        ResponseEntity<ErrorEnvelope> response = advice.handleObjectStorage(
                request,
                new ObjectStorageException("Failed to read object", new RuntimeException("io"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.INTERNAL_ERROR);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.storage.operationFailed");
    }

    @Test
    void docxAssemblyExceptionMapsToRenderingFailed() {
        when(messageResolver.resolve("api.error.rendering.generationFailed"))
                .thenReturn("Document generation failed.");

        ResponseEntity<ErrorEnvelope> response = advice.handleDocxAssembly(
                request,
                new DocxAssemblyException(new RuntimeException("docx"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.RENDERING_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.RENDERING);
    }

    @Test
    void docxAssemblyExceptionMapsContentModuleStructureMissingToValidation() {
        when(messageResolver.resolve("api.error.validation.contentModuleStructureMissing"))
                .thenReturn("The referenced content module has no pinned structure.");

        ResponseEntity<ErrorEnvelope> response = advice.handleDocxAssembly(
                request,
                new DocxAssemblyException(
                        ApiErrorCodes.CONTENT_MODULE_STRUCTURE_MISSING,
                        ApiErrorCategories.VALIDATION,
                        "api.error.validation.contentModuleStructureMissing",
                        "Content module pinned structure is missing for reference: CLAUSE-1"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.CONTENT_MODULE_STRUCTURE_MISSING);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.VALIDATION);
        assertThat(response.getBody().error().retryable()).isFalse();
    }

    @Test
    void renderingOperationExceptionMapsToRenderingFailed() {
        when(messageResolver.resolve("api.error.generation.pdfConversionFailed"))
                .thenReturn("PDF conversion failed.");

        ResponseEntity<ErrorEnvelope> response = advice.handleRenderingOperation(
                request,
                new RenderingOperationException("api.error.generation.pdfConversionFailed")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.RENDERING_FAILED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.GENERATION);
        assertThat(response.getBody().error().messageKey()).isEqualTo("api.error.generation.pdfConversionFailed");
    }

    @Test
    void previewPath_compositionInclusionUnsatisfied_mapsToStable422NotRenderingFailed() {
        when(messageResolver.resolve("api.error.template.compositionInclusionUnsatisfied"))
                .thenReturn("Composition inclusion requirements were not satisfied.");

        ResponseEntity<ErrorEnvelope> response = advice.handlePreviewGeneration(
                request,
                new PreviewGenerationException(
                        "api.error.rendering.generationFailed",
                        new CompositionInclusionUnsatisfiedException()
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.COMPOSITION_INCLUSION_UNSATISFIED);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.TEMPLATE);
        assertThat(response.getBody().error().code()).isNotEqualTo(ApiErrorCodes.RENDERING_FAILED);
    }

    @Test
    void previewPath_contentModuleJurisdictionMismatch_mapsToStable422NotRenderingFailed() {
        when(messageResolver.resolve("api.error.template.contentModuleJurisdictionMismatch"))
                .thenReturn("Content module jurisdiction does not match request context.");

        ResponseEntity<ErrorEnvelope> response = advice.handlePreviewGeneration(
                request,
                new PreviewGenerationException(
                        "api.error.rendering.generationFailed",
                        new ContentModuleJurisdictionMismatchException()
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.CONTENT_MODULE_JURISDICTION_MISMATCH);
        assertThat(response.getBody().error().category()).isEqualTo(ApiErrorCategories.TEMPLATE);
        assertThat(response.getBody().error().code()).isNotEqualTo(ApiErrorCodes.RENDERING_FAILED);
    }

    @Test
    void compositionInclusionUnsatisfied_directHandler_mapsToStable422() {
        when(messageResolver.resolve("api.error.template.compositionInclusionUnsatisfied"))
                .thenReturn("Composition inclusion requirements were not satisfied.");

        ResponseEntity<ErrorEnvelope> response = advice.handleCompositionInclusionUnsatisfied(
                request,
                new CompositionInclusionUnsatisfiedException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error().code()).isEqualTo(ApiErrorCodes.COMPOSITION_INCLUSION_UNSATISFIED);
    }
}
