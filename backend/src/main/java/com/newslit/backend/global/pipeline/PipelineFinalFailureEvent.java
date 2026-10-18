package com.newslit.backend.global.pipeline;

import com.newslit.backend.article.ArticlePipelineStage;
import java.time.LocalDateTime;

public record PipelineFinalFailureEvent(
        Long articleId,
        ArticlePipelineStage stage,
        int retryCount,
        String reason,
        LocalDateTime occurredAt
) {
}
