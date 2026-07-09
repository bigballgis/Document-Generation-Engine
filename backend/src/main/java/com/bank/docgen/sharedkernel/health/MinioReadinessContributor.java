package com.bank.docgen.sharedkernel.health;

import com.bank.docgen.infrastructure.storage.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "docgen.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioReadinessContributor implements ComponentReadinessContributor {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioReadinessContributor(StorageProperties storageProperties) {
        StorageProperties.MinioProperties minio = storageProperties.minio();
        this.bucket = storageProperties.bucket();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.endpoint())
                .credentials(minio.accessKey(), minio.secretKey())
                .build();
    }

    @Override
    public String componentName() {
        return "minio";
    }

    @Override
    public ComponentCheck check() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            return new ComponentCheck(exists ? "UP" : "DOWN", null);
        } catch (RuntimeException | ErrorResponseException | InsufficientDataException
                 | InternalException | InvalidKeyException | InvalidResponseException
                 | IOException | NoSuchAlgorithmException | ServerException | XmlParserException ex) {
            return new ComponentCheck("DOWN", null);
        }
    }
}
