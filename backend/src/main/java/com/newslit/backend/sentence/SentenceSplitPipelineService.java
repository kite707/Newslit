package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentenceSplitPipelineService {
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final ArticleRepository articleRepository;
    private final SentenceService sentenceService;
    private final SentenceSplitPipelineStateService stateService;
    private final SentenceSplitPipelineProperties properties;
    private final PipelineFailureEventPublisher failureEventPublisher;

    public int processBatch() {
        List<Long> articleIds = articleRepository.findPipelineCandidateIds(
                ArticlePipelineStage.SENTENCE_SPLIT,
                CLAIMABLE_STATUSES,
                properties.getMaxRetryCount(),
                PageRequest.of(0, properties.getBatchSize())
        );

        int claimedCount = 0;
        for (Long articleId : articleIds) {
            if (!stateService.claim(articleId, properties.getMaxRetryCount())) {
                continue;
            }

            claimedCount++;
            processClaimedArticle(articleId);
        }

        log.info("문장 분리 배치 처리 완료 - 조회: {}건, 선점: {}건", articleIds.size(), claimedCount);
        return claimedCount;
    }

    public int recoverStaleProcessing() {
        return recoverStaleProcessing(
                LocalDateTime.now().minusMinutes(properties.getStaleProcessingMinutes())
        );
    }

    public int recoverStaleProcessing(LocalDateTime threshold) {
        List<Long> articleIds = articleRepository.findStalePipelineCandidateIds(
                ArticlePipelineStage.SENTENCE_SPLIT,
                Status.PROCESSING,
                properties.getMaxRetryCount(),
                threshold,
                PageRequest.of(0, properties.getBatchSize())
        );

        int recoveredCount = 0;
        for (Long articleId : articleIds) {
            SentenceSplitPipelineStateService.SplitFailure failure = stateService.recoverStale(
                    articleId,
                    threshold,
                    properties.getMaxRetryCount()
            );
            if (failure == null) {
                continue;
            }

            recoveredCount++;
            log.warn("pipelineRecovery=true articleId={} stage={} retryCount={} reason=stale-processing",
                    articleId, ArticlePipelineStage.SENTENCE_SPLIT, failure.retryCount());
            publishIfExhausted(failure, "stale-processing");
        }
        return recoveredCount;
    }

    private void processClaimedArticle(Long articleId) {
        try {
            int sentenceCount = sentenceService.splitSentences(articleId).size();
            if (sentenceCount == 0) {
                throw new IllegalStateException("분리된 문장이 없습니다");
            }
            log.info("문장 분리 성공 - articleId: {}, sentenceCount: {}", articleId, sentenceCount);
        } catch (Exception e) {
            SentenceSplitPipelineStateService.SplitFailure failure = stateService.fail(
                    articleId,
                    properties.getMaxRetryCount()
            );
            int retryCount = failure == null ? -1 : failure.retryCount();
            log.error("pipelineFailure=true articleId={} stage={} retryCount={} reason={}",
                    articleId, ArticlePipelineStage.SENTENCE_SPLIT, retryCount, e.getMessage(), e);
            publishIfExhausted(failure, e.getMessage());
        }
    }

    private void publishIfExhausted(
            SentenceSplitPipelineStateService.SplitFailure failure,
            String reason
    ) {
        if (failure == null || !failure.exhausted()) {
            return;
        }

        failureEventPublisher.publish(
                failure.articleId(),
                ArticlePipelineStage.SENTENCE_SPLIT,
                failure.retryCount(),
                reason
        );
    }
}
