package com.newslit.backend.sentence;

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
public class SentenceSplitPipelineStateService {
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final ArticleRepository articleRepository;

    @Transactional
    public boolean claim(Long articleId, int maxRetryCount) {
        return articleRepository.claimSentenceSplit(
                articleId,
                ArticlePipelineStage.SENTENCE_SPLIT,
                CLAIMABLE_STATUSES,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public SplitFailure fail(Long articleId, int maxRetryCount) {
        int updated = articleRepository.failSentenceSplit(
                articleId,
                ArticlePipelineStage.SENTENCE_SPLIT,
                Status.PROCESSING,
                Status.FAILED,
                maxRetryCount,
                LocalDateTime.now()
        );
        return failureResult(articleId, maxRetryCount, updated);
    }

    @Transactional
    public SplitFailure recoverStale(Long articleId, LocalDateTime threshold, int maxRetryCount) {
        int updated = articleRepository.recoverStaleSentenceSplit(
                articleId,
                ArticlePipelineStage.SENTENCE_SPLIT,
                Status.PROCESSING,
                Status.FAILED,
                maxRetryCount,
                threshold,
                LocalDateTime.now()
        );
        return failureResult(articleId, maxRetryCount, updated);
    }

    private SplitFailure failureResult(Long articleId, int maxRetryCount, int updated) {
        if (updated == 0) {
            return null;
        }

        int retryCount = articleRepository.findById(articleId)
                .orElseThrow()
                .getPipelineRetryCount();
        return new SplitFailure(articleId, retryCount, retryCount >= maxRetryCount);
    }

    public record SplitFailure(Long articleId, int retryCount, boolean exhausted) {
    }
}
