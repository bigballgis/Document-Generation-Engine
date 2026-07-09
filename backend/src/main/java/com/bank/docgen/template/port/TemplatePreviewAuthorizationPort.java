package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.UUID;

/**
 * Readable-template and preview-author authorization for rendering orchestration.
 */
public interface TemplatePreviewAuthorizationPort {

    RenderableTemplateSnapshot requireReadableSnapshot(UUID templateId, ManagementSessionClaims session);

    void requirePreviewAuthor(ManagementSessionClaims session);
}
