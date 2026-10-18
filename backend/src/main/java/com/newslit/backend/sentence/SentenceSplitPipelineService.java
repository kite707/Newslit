package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
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

    /**
     * 운영자가 단건으로 문장 분리를 돌릴 때 쓰는 경로.
     * 스케줄러와 같은 선점 프로토콜을 타므로, 이미 다른 인스턴스가 처리 중인 기사는 건드리지 않는다.
     *
     * @return 이번 호출이 기사를 선점해 처리했으면 true
     */
    public boolean processArticle(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new ArticleNotFoundException();
        }
        if (!stateService.claim(articleId, properties.getMaxRetryCount())) {
            log.info("문장 분리 선점 실패 - 처리 중이거나 이미 다음 단계입니다. articleId: {}", articleId);
            return false;
        }

        processClaimedArticle(articleId);
        return true;
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
            if (!stateService.isSplitCompleted(articleId)) {
                throw new IllegalStateException("문장 분리 완료 상태 전이에 실패했습니다");
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
