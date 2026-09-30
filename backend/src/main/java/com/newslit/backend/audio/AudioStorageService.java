package com.newslit.backend.audio;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudioStorageService {

    @Value("${oracle.cloud.namespace}")
    private String namespace;
    @Value("${oracle.cloud.bucket}")
    private String bucket;
    @Value("${oracle.cloud.region}")
    private String region;

    private final ObjectStorage objectStorage;

    public String uploadMp3(String objectName, byte[] data) {
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectName)
                .contentLength((long) data.length)
                .contentType("audio/mpeg")
                .putObjectBody(new ByteArrayInputStream(data))
                .build();

        objectStorage.putObject(request);

        return String.format(
                "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region, namespace, bucket, objectName
        );
    }
}
