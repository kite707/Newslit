package com.newslit.backend.daily;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyPipelineStateService {
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final ArticleRepository articleRepository;

    @Transactional
    public boolean claim(Long articleId, int maxRetryCount) {
        return articleRepository.claimDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                CLAIMABLE_STATUSES,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public boolean complete(Long articleId) {
        return articleRepository.completeDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                ArticlePipelineStage.COMPLETED,
                Status.PROCESSING,
                Status.SUCCESS,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public DailyFailure fail(Long articleId, int maxRetryCount) {
        int updated = articleRepository.failDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PROCESSING,
                Status.FAILED,
                maxRetryCount,
                LocalDateTime.now()
        );
        return failureResult(articleId, maxRetryCount, updated);
    }

    @Transactional
    public DailyFailure recoverStale(Long articleId, LocalDateTime threshold, int maxRetryCount) {
        int updated = articleRepository.recoverStaleDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PROCESSING,
                Status.FAILED,
                maxRetryCount,
                threshold,
                LocalDateTime.now()
        );
        return failureResult(articleId, maxRetryCount, updated);
    }

    private DailyFailure failureResult(Long articleId, int maxRetryCount, int updated) {
        if (updated == 0) {
            return null;
        }

        int retryCount = articleRepository.findById(articleId)
                .orElseThrow()
                .getPipelineRetryCount();
        return new DailyFailure(articleId, retryCount, retryCount >= maxRetryCount);
    }

    public record DailyFailure(Long articleId, int retryCount, boolean exhausted) {
    }
}
