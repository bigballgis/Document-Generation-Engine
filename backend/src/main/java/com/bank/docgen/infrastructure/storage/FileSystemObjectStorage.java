package com.bank.docgen.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.storage.provider", havingValue = "filesystem")
public class FileSystemObjectStorage implements ObjectStoragePort {

    private final Path root;

    public FileSystemObjectStorage(StorageProperties storageProperties) {
        this.root = Path.of(System.getProperty("java.io.tmpdir"), "docgen-storage", storageProperties.bucket());
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new ObjectStorageException("Failed to initialize filesystem storage", ex);
        }
    }

    @Override
    public void put(String objectKey, InputStream content, long contentLength, String contentType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
        } catch (IOException ex) {
            throw new ObjectStorageException("Failed to store object", ex);
        }
    }

    @Override
    public InputStream get(String objectKey) {
        try {
            return Files.newInputStream(resolve(objectKey));
        } catch (IOException ex) {
            throw new ObjectStorageException("Failed to read object", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException ex) {
            throw new ObjectStorageException("Failed to delete object", ex);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolve(objectKey));
    }

    private Path resolve(String objectKey) {
        return root.resolve(objectKey.replace("\\", "/"));
    }
}
