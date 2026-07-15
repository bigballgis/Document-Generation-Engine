package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.FieldError;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-C05-001 / 005 / 006 / 007 / 011 — originalBatchId lineage validation.
 */
@ExtendWith(MockitoExtension.class)
class OriginalBatchLineageValidatorTest {

    private static final UUID CREDENTIAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ApiInvocationRecordRepository repository;

    private OriginalBatchLineageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OriginalBatchLineageValidator(repository);
    }

    @Test
    void bddCeC05_001_omittedOriginalBatchIdSkipsLookup() {
        assertThatCode(() -> validator.requireValidOriginalBatchIfPresent(null, CREDENTIAL_ID))
                .doesNotThrowAnyException();

        verify(repository, never()).countByBatchExternalIdAndInvocationKindAndCredentialId(
                any(), any(), any());
    }

    @Test
    void bddCeC05_005_missingOriginalBatchThrowsNotFound() {
        when(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                "BATCH-MISSING1", InvocationKind.BATCH_ROOT, CREDENTIAL_ID
        )).thenReturn(0L);

        assertThatThrownBy(() -> validator.requireValidOriginalBatchIfPresent("BATCH-MISSING1", CREDENTIAL_ID))
                .isInstanceOf(OriginalBatchNotFoundException.class)
                .extracting(ex -> ((OriginalBatchNotFoundException) ex).errorCode(),
                        ex -> ((OriginalBatchNotFoundException) ex).messageKey())
                .containsExactly(
                        ApiErrorCodes.ORIGINAL_BATCH_NOT_FOUND,
                        "api.error.batch.originalBatchNotFound");
    }

    @Test
    void bddCeC05_006_otherCredentialLooksLikeMissing() {
        when(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                "BATCH-OTHER1", InvocationKind.BATCH_ROOT, CREDENTIAL_ID
        )).thenReturn(0L);

        assertThatThrownBy(() -> validator.requireValidOriginalBatchIfPresent("BATCH-OTHER1", CREDENTIAL_ID))
                .isInstanceOf(OriginalBatchNotFoundException.class);
    }

    @Test
    void bddCeC05_007_badPatternThrowsFormatInvalid() {
        assertThatThrownBy(() -> validator.requireValidOriginalBatchIfPresent("not-a-batch", CREDENTIAL_ID))
                .isInstanceOf(OriginalBatchIdFormatException.class)
                .satisfies(ex -> assertThat(((OriginalBatchIdFormatException) ex).fieldErrors())
                        .extracting(FieldError::field, FieldError::reason)
                        .containsExactly(org.assertj.core.groups.Tuple.tuple(
                                "originalBatchId", "PATTERN_MISMATCH")));

        verify(repository, never()).countByBatchExternalIdAndInvocationKindAndCredentialId(
                any(), any(), any());
    }

    @Test
    void bddCeC05_007_emptyStringThrowsFormatInvalid() {
        assertThatThrownBy(() -> validator.requireValidOriginalBatchIfPresent("", CREDENTIAL_ID))
                .isInstanceOf(OriginalBatchIdFormatException.class);

        verify(repository, never()).countByBatchExternalIdAndInvocationKindAndCredentialId(
                any(), any(), any());
    }

    @Test
    void bddCeC05_011_nonRootIdThrowsNotFound() {
        when(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                eq("BATCH-ITEMONLY"), eq(InvocationKind.BATCH_ROOT), eq(CREDENTIAL_ID)
        )).thenReturn(0L);

        assertThatThrownBy(() -> validator.requireValidOriginalBatchIfPresent("BATCH-ITEMONLY", CREDENTIAL_ID))
                .isInstanceOf(OriginalBatchNotFoundException.class);
    }

    @Test
    void validSameCredentialBatchRootPasses() {
        when(repository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                "BATCH-ORIG01", InvocationKind.BATCH_ROOT, CREDENTIAL_ID
        )).thenReturn(1L);

        assertThatCode(() -> validator.requireValidOriginalBatchIfPresent("BATCH-ORIG01", CREDENTIAL_ID))
                .doesNotThrowAnyException();
    }
}
