package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructuredContentImageResolverTest {

    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Test
    void resolvesImageRefFromMinioWhenObjectExists() {
        when(objectStoragePort.exists("IMG-1")).thenReturn(true);
        when(objectStoragePort.get("IMG-1")).thenReturn(new ByteArrayInputStream(PNG_BYTES));

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);

        StructuredContentImageResolver.ResolvedImage image = resolver.resolveImageRef("IMG-1");

        assertThat(image.bytes()).isEqualTo(PNG_BYTES);
        assertThat(image.fileName()).isEqualTo("IMG-1.png");
    }

    @Test
    void resolvesSealRefFromMinioWhenObjectExists() {
        when(objectStoragePort.exists("SEAL-1")).thenReturn(true);
        when(objectStoragePort.get("SEAL-1")).thenReturn(new ByteArrayInputStream(PNG_BYTES));

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);

        StructuredContentImageResolver.ResolvedImage image = resolver.resolveSealRef("SEAL-1");

        assertThat(image.bytes()).isEqualTo(PNG_BYTES);
        assertThat(image.fileName()).isEqualTo("SEAL-1.png");
    }

    @Test
    void failsClosedWhenImageAssetMissingAndDemoTierDisabled() {
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);

        assertThatThrownBy(() -> resolver.resolveImageRef("MISSING-IMAGE"))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.errorCode()).isEqualTo(ApiErrorCodes.IMAGE_ASSET_NOT_FOUND);
                    assertThat(assemblyException.category()).isEqualTo(ApiErrorCategories.RENDERING);
                    assertThat(assemblyException.messageKey()).isEqualTo("api.error.rendering.imageAssetNotFound");
                });
    }

    @Test
    void failsClosedWhenSealAssetMissingAndDemoTierDisabled() {
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);

        assertThatThrownBy(() -> resolver.resolveSealRef("MISSING-SEAL"))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.errorCode()).isEqualTo(ApiErrorCodes.SEAL_ASSET_NOT_FOUND);
                    assertThat(assemblyException.category()).isEqualTo(ApiErrorCategories.RENDERING);
                    assertThat(assemblyException.messageKey()).isEqualTo("api.error.rendering.sealAssetNotFound");
                });
    }

    @Test
    void resolvesFromDemoClasspathTierWhenEnabledAndMinioMissing() {
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, true);

        StructuredContentImageResolver.ResolvedImage image = resolver.resolveImageRef("IMG-1");

        assertThat(image.bytes()).isNotEmpty();
        assertThat(image.fileName()).contains("IMG-1");
    }

    @Test
    void writeReferenceNodeFailsClosedWhenImageMissing() throws Exception {
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);
        StructuredContentDocxWriter writer = StructuredContentDocxWriterTestSupport.createWriter(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                resolver
        );
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"imageRef","imageRef":"MISSING-IMAGE"}]}]}
                """;

        assertThatThrownBy(() -> StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structured,
                Map.of(),
                Map.of()
        ))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.IMAGE_ASSET_NOT_FOUND);
    }

    @Test
    void writeReferenceNodeFailsClosedWhenSealMissing() throws Exception {
        when(objectStoragePort.exists(anyString())).thenReturn(false);

        StructuredContentImageResolver resolver = new StructuredContentImageResolver(objectStoragePort, false);
        StructuredContentDocxWriter writer = StructuredContentDocxWriterTestSupport.createWriter(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                resolver
        );
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"sealRef","referenceKey":"MISSING-SEAL"}]}]}
                """;

        assertThatThrownBy(() -> StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer,
                structured,
                Map.of(),
                Map.of()
        ))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.SEAL_ASSET_NOT_FOUND);
    }
}
