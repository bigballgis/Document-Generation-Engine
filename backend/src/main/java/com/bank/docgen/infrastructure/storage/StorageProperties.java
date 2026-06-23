package com.bank.docgen.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.storage")
public record StorageProperties(
        String provider,
        String bucket,
        MinioProperties minio
) {
    public record MinioProperties(String endpoint, String accessKey, String secretKey) {
    }
}
