package com.bank.docgen.runtime.service;

import java.util.UUID;

/**
 * CE-G06 release-bundle fingerprint captured on invocation write.
 *
 * @param snapshotId {@code template_version.id} at generation time
 * @param bundleHash copy of {@code template_version.master_file_hash}
 */
public record ReleaseBundleFingerprint(UUID snapshotId, String bundleHash) {
}
