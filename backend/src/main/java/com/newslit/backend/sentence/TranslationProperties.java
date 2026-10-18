package com.newslit.backend.sentence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeline.translation")
@Getter
@Setter
public class TranslationProperties {
    private int batchSize = 20;
    private int maxRetryCount = 10;
    private long fixedDelayMs = 300_000;
    private long staleProcessingMinutes = 10;
}
