package com.newslit.backend.sentence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeline.sentence-split")
@Getter
@Setter
public class SentenceSplitPipelineProperties {
    private int batchSize = 5;
    private int maxRetryCount = 3;
    private long fixedDelayMs = 300_000;
    private long staleProcessingMinutes = 10;
}
