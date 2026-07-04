package com.bank.docgen.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.storage")
@SuppressWarnings("PMD.UnusedAssignment")
public record StorageProperties(
        String provider,
        String bucket,
        MinioProperties minio
) {
    public StorageProperties {
        minio = minio == null ? null : new MinioProperties(minio.endpoint(), minio.accessKey(), minio.secretKey());
    }

    public record MinioProperties(String endpoint, String accessKey, String secretKey) {
    }
}
