package com.bank.docgen.infrastructure.storage;

import com.bank.docgen.infrastructure.resilience.ResilienceSupport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorage implements ObjectStoragePort {

    private static final Logger LOG = LoggerFactory.getLogger(MinioObjectStorage.class);
    private static final String RESILIENCE_INSTANCE = "objectStorage";

    private final MinioClient minioClient;
    private final String bucket;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public MinioObjectStorage(
            StorageProperties storageProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        StorageProperties.MinioProperties minio = storageProperties.minio();
        this.bucket = storageProperties.bucket();
        this.minioClient = MinioClient.builder()
                .endpoint(minio.endpoint())
                .credentials(minio.accessKey(), minio.secretKey())
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE);
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE);
        ensureBucketExists();
    }

    @Override
    public void put(String objectKey, InputStream content, long contentLength, String contentType) {
        ResilienceSupport.executeVoid(circuitBreaker, retry, () -> putInternal(objectKey, content, contentLength, contentType));
    }

    @Override
    public InputStream get(String objectKey) {
        return ResilienceSupport.execute(circuitBreaker, retry, () -> getInternal(objectKey));
    }

    @Override
    public void delete(String objectKey) {
        ResilienceSupport.executeVoid(circuitBreaker, retry, () -> deleteInternal(objectKey));
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() == null ? "" : ex.errorResponse().code();
            if (isNotFoundCode(code)) {
                LOG.debug("Object not found for key {}: {}", objectKey, code);
                return false;
            }
            throw new ObjectStorageException("Failed to stat object", ex);
        } catch (IOException
                | InvalidKeyException
                | NoSuchAlgorithmException
                | InsufficientDataException
                | InternalException
                | InvalidResponseException
                | ServerException
                | XmlParserException ex) {
            throw new ObjectStorageException("Failed to stat object", ex);
        }
    }

    private static boolean isNotFoundCode(String code) {
        return "NoSuchKey".equals(code)
                || "NotFound".equals(code)
                || "NoSuchObject".equals(code)
                || "ResourceNotFound".equals(code);
    }

    private void putInternal(String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, contentLength, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new ObjectStorageException("Failed to store object", ex);
        }
    }

    private InputStream getInternal(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new ObjectStorageException("Failed to read object", ex);
        }
    }

    private void deleteInternal(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new ObjectStorageException("Failed to delete object", ex);
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new ObjectStorageException("Failed to initialize storage bucket", ex);
        }
    }
}
