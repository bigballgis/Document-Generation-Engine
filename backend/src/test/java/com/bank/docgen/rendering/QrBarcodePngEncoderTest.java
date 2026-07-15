package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import org.junit.jupiter.api.Test;

/**
 * CE-K06b follow-up — encode failure details must stay ADR-0020-safe (no raw exception messages).
 */
class QrBarcodePngEncoderTest {

    @Test
    void encodeFailure_usesSafeDetailWithoutRawExceptionMessage() {
        QrBarcodeRefConfig config = new QrBarcodeRefConfig(
                QrBarcodeRefConfig.DEFAULT_SIZE_PX,
                QrBarcodeRefConfig.ErrorCorrection.M,
                QrBarcodeRefConfig.BarcodeFormatKind.QR_CODE
        );

        // ZXing rejects empty contents with IllegalArgumentException("Found empty contents").
        assertThatThrownBy(() -> QrBarcodePngEncoder.encode("", config))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assembly = (DocxAssemblyException) ex;
                    assertThat(assembly.messageKey()).isEqualTo("api.error.rendering.qrBarcodeEncodeFailed");
                    assertThat(assembly.errorCode()).isEqualTo(ApiErrorCodes.RENDERING_FAILED);
                    assertThat(assembly.category()).isEqualTo(ApiErrorCategories.RENDERING);
                    assertThat(assembly.getMessage())
                            .isEqualTo("Failed to encode qrBarcodeRef payload (IllegalArgumentException)");
                    assertThat(assembly.getMessage()).doesNotContain("Found empty contents");
                });
    }
}
