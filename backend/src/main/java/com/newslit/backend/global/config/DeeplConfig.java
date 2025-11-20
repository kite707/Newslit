package com.newslit.backend.global.config;

import com.deepl.api.DeepLClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeeplConfig {
    @Value("${deepl.api-key}")
    private String apiKey;

    @Bean
    public DeepLClient deepLClient() {
        return new DeepLClient(apiKey);
    }
}
