package com.newslit.backend.sentence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeline.tts")
@Getter
@Setter
public class TtsProperties {
    private int batchSize = 10;
    private int maxRetryCount = 3;
    private long fixedDelayMs = 300_000;
    private long staleProcessingMinutes = 10;
}
