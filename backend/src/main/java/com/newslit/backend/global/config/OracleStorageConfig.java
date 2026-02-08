package com.newslit.backend.global.config;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OracleStorageConfig {

    @Value("${oracle.cloud.user}")
    private String userId;

    @Value("${oracle.cloud.tenancy}")
    private String tenancyId;

    @Value("${oracle.cloud.fingerprint}")
    private String fingerprint;

    @Value("${oracle.cloud.region}")
    private String region;

    @Value("${oracle.cloud.key-file}")
    private String keyFile;

    @Bean
    public ObjectStorage objectStorageClient() throws IOException {
        System.out.println("=== Oracle Cloud Config ===");
        System.out.println("Key file path: " + keyFile);
        System.out.println("User ID: " + userId);
        System.out.println("Region: " + region);
        System.out.println("===========================");
        Supplier<InputStream> privateKeySupplier = () -> {
            try {
                return new FileInputStream(keyFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        };

        AuthenticationDetailsProvider provider =
                SimpleAuthenticationDetailsProvider.builder()
                        .userId(userId)
                        .tenantId(tenancyId)
                        .fingerprint(fingerprint)
                        .privateKeySupplier(privateKeySupplier)
                        .region(Region.fromRegionId(region))
                        .build();

        return ObjectStorageClient.builder()
                .build(provider);
    }
}