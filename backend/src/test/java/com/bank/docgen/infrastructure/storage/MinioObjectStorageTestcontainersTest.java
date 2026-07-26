package com.bank.docgen.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * FOS-W13-5 — MinIO adapter put/get/delete against Testcontainers MinIO.
 */
@Tag("testcontainers")
@Testcontainers
class MinioObjectStorageTestcontainersTest {

    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2024-12-18T13-15-44Z";
    private static final String ACCESS = "minioadmin";
    private static final String SECRET = "minioadmin";
    private static final String BUCKET = "docgen-test";

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withEnv("MINIO_ROOT_USER", ACCESS)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET)
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    private MinioObjectStorage storage;

    @BeforeEach
    void setUp() {
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        StorageProperties properties = new StorageProperties(
                "minio",
                BUCKET,
                new StorageProperties.MinioProperties(endpoint, ACCESS, SECRET)
        );
        storage = new MinioObjectStorage(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults()
        );
    }

    @Test
    void putGetDeleteRoundTrip() throws Exception {
        byte[] payload = "fos-w13-minio".getBytes(StandardCharsets.UTF_8);
        String key = "w13/minio-roundtrip.txt";

        storage.put(key, new ByteArrayInputStream(payload), payload.length, "text/plain");
        assertThat(storage.exists(key)).isTrue();
        try (var in = storage.get(key)) {
            assertThat(in.readAllBytes()).isEqualTo(payload);
        }

        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }
}
