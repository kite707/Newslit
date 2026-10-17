package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TranslationFailureService {
    private static final List<Status> ACTIVE_PIPELINE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);

    private final ArticleRepository articleRepository;
    private final PipelineFailureEventPublisher failureEventPublisher;

    @Transactional
    public boolean finalizeIfExhausted(
            TranslationStateService.TranslationFailure failure,
            int maxRetryCount,
            String reason
    ) {
        if (failure == null || failure.retryCount() < maxRetryCount) {
            return false;
        }

        int updated = articleRepository.failTranslationPipeline(
                failure.articleId(),
                ArticlePipelineStage.TRANSLATION,
                ACTIVE_PIPELINE_STATUSES,
                Status.FAILED,
                LocalDateTime.now()
        );
        if (updated == 0) {
            return false;
        }

        failureEventPublisher.publish(
                failure.articleId(),
                ArticlePipelineStage.TRANSLATION,
                failure.retryCount(),
                reason
        );
        return true;
    }
}
