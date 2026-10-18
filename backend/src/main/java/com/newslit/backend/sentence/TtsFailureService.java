package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TtsFailureService {

    private final PipelineFailureEventPublisher failureEventPublisher;

    /**
     * 오디오가 없어도 Daily는 그대로 제공되므로, TTS가 최종 실패해도 Article 파이프라인 상태는
     * 바꾸지 않고 알림(#42)용 이벤트만 발행한다.
     */
    public boolean notifyIfExhausted(TtsStateService.TtsFailure failure, int maxRetryCount, String reason) {
        if (failure == null || failure.retryCount() < maxRetryCount) {
            return false;
        }

        failureEventPublisher.publish(
                failure.articleId(),
                ArticlePipelineStage.TTS,
                failure.retryCount(),
                reason
        );
        return true;
    }
}
