package com.newslit.backend.daily;

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
public class DailyPipelineService {
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final ArticleRepository articleRepository;
    private final DailyPipelineProperties properties;
    private final DailyPipelineStateService stateService;
    private final DailyService dailyService;
    private final PipelineFailureEventPublisher failureEventPublisher;

    public int processBatch() {
        List<Long> articleIds = articleRepository.findPipelineCandidateIds(
                ArticlePipelineStage.DAILY_CHUNK,
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

        log.info("Daily 배치 처리 완료 - 조회: {}건, 선점: {}건", articleIds.size(), claimedCount);
        return claimedCount;
    }

    public int recoverStaleProcessing() {
        return recoverStaleProcessing(
                LocalDateTime.now().minusMinutes(properties.getStaleProcessingMinutes())
        );
    }

    int recoverStaleProcessing(LocalDateTime threshold) {
        List<Long> articleIds = articleRepository.findStalePipelineCandidateIds(
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PROCESSING,
                properties.getMaxRetryCount(),
                threshold,
                PageRequest.of(0, properties.getBatchSize())
        );

        int recoveredCount = 0;
        for (Long articleId : articleIds) {
            DailyPipelineStateService.DailyFailure failure = stateService.recoverStale(
                    articleId,
                    threshold,
                    properties.getMaxRetryCount()
            );
            if (failure == null) {
                continue;
            }

            recoveredCount++;
            log.warn("pipelineRecovery=true articleId={} stage={} retryCount={} reason=stale-processing",
                    articleId, ArticlePipelineStage.DAILY_CHUNK, failure.retryCount());
            publishIfExhausted(failure, "stale-processing");
        }
        return recoveredCount;
    }

    private void processClaimedArticle(Long articleId) {
        try {
            int chunkCount = dailyService.createChunks(articleId).size();
            if (chunkCount == 0) {
                throw new IllegalStateException("생성된 Daily 청크가 없습니다");
            }
            if (!stateService.complete(articleId)) {
                throw new IllegalStateException("Daily 완료 상태 전이에 실패했습니다");
            }
            log.info("Daily 생성 성공 - articleId: {}, chunkCount: {}", articleId, chunkCount);
        } catch (Exception e) {
            DailyPipelineStateService.DailyFailure failure = stateService.fail(
                    articleId,
                    properties.getMaxRetryCount()
            );
            int retryCount = failure == null ? -1 : failure.retryCount();
            log.error("pipelineFailure=true articleId={} stage={} retryCount={} reason={}",
                    articleId, ArticlePipelineStage.DAILY_CHUNK, retryCount, e.getMessage(), e);
            publishIfExhausted(failure, e.getMessage());
        }
    }

    private void publishIfExhausted(DailyPipelineStateService.DailyFailure failure, String reason) {
        if (failure == null || !failure.exhausted()) {
            return;
        }

        failureEventPublisher.publish(
                failure.articleId(),
                ArticlePipelineStage.DAILY_CHUNK,
                failure.retryCount(),
                reason
        );
    }
}
