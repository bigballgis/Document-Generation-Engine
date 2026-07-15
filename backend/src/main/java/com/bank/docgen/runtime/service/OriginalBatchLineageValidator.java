package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * CE-C05: validate optional {@code originalBatchId} retry lineage against same-credential BATCH_ROOT.
 */
@Component
public class OriginalBatchLineageValidator {

    private static final Pattern BATCH_ID_PATTERN = Pattern.compile("^BATCH-[A-Za-z0-9]+$");

    private final ApiInvocationRecordRepository invocationRecordRepository;

    public OriginalBatchLineageValidator(ApiInvocationRecordRepository invocationRecordRepository) {
        this.invocationRecordRepository = invocationRecordRepository;
    }

    public void requireValidOriginalBatchIfPresent(String originalBatchId, UUID credentialId) {
        if (originalBatchId == null) {
            return;
        }
        if (!BATCH_ID_PATTERN.matcher(originalBatchId).matches()) {
            throw new OriginalBatchIdFormatException();
        }
        long count = invocationRecordRepository.countByBatchExternalIdAndInvocationKindAndCredentialId(
                originalBatchId,
                InvocationKind.BATCH_ROOT,
                credentialId
        );
        if (count == 0) {
            throw new OriginalBatchNotFoundException();
        }
    }
}
