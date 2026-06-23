package com.bank.docgen.infrastructure.storage;

import java.io.InputStream;

public interface ObjectStoragePort {

    void put(String objectKey, InputStream content, long contentLength, String contentType);

    InputStream get(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
