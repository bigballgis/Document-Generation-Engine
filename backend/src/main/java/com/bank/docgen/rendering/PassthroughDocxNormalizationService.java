package com.bank.docgen.rendering;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "docgen.rendering.docx-normalization-enabled", havingValue = "false", matchIfMissing = true)
public class PassthroughDocxNormalizationService implements DocxNormalizationService {

    @Override
    public byte[] normalize(byte[] docxBytes) {
        return docxBytes;
    }
}
