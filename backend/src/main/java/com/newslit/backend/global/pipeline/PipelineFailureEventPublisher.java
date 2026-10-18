package com.newslit.backend.global.pipeline;

import com.newslit.backend.article.ArticlePipelineStage;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineFailureEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    // 최종 실패 알림(#42)은 이 이벤트의 리스너로 연결한다.
    public void publish(Long articleId, ArticlePipelineStage stage, int retryCount, String reason) {
        eventPublisher.publishEvent(new PipelineFinalFailureEvent(
                articleId,
                stage,
                retryCount,
                reason,
                LocalDateTime.now()
        ));
    }
}
