package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationRecoveryService {
    private static final List<Status> ACTIVE_PIPELINE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);

    private final SentenceRepository sentenceRepository;
    private final TranslationProperties properties;
    private final TranslationStateService stateService;
    private final TranslationFailureService failureService;

    public int recoverStaleProcessing() {
        return recoverStaleProcessing(
                LocalDateTime.now().minusMinutes(properties.getStaleProcessingMinutes())
        );
    }

    int recoverStaleProcessing(LocalDateTime threshold) {
        List<SentenceRepository.TranslationCandidate> candidates = sentenceRepository
                .findStaleTranslationCandidates(
                        ArticlePipelineStage.TRANSLATION,
                        ACTIVE_PIPELINE_STATUSES,
                        Status.PROCESSING,
                        properties.getMaxRetryCount(),
                        threshold,
                        PageRequest.of(0, properties.getBatchSize())
                );

        int recoveredCount = 0;
        for (SentenceRepository.TranslationCandidate candidate : candidates) {
            TranslationStateService.TranslationFailure failure = stateService.recoverStale(
                    candidate.getSentenceId(),
                    threshold,
                    properties.getMaxRetryCount()
            );
            if (failure == null) {
                continue;
            }

            recoveredCount++;
            log.warn("pipelineRecovery=true articleId={} sentenceId={} stage={} retryCount={} reason=stale-processing",
                    failure.articleId(),
                    failure.sentenceId(),
                    ArticlePipelineStage.TRANSLATION,
                    failure.retryCount());
            failureService.finalizeIfExhausted(
                    failure,
                    properties.getMaxRetryCount(),
                    "stale-processing sentenceId=" + failure.sentenceId()
            );
        }
        return recoveredCount;
    }
}
