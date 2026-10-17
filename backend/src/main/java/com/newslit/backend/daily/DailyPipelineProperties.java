package com.newslit.backend.daily;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pipeline.daily")
@Getter
@Setter
public class DailyPipelineProperties {
    private int batchSize = 5;
    private long fixedDelayMs = 300_000;
}
