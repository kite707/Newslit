package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TtsStateService {

    private final SentenceRepository sentenceRepository;
    private final PipelineFailureEventPublisher failureEventPublisher;

    @Transactional(readOnly = true)
    public TtsWork getWork(Long sentenceId) {
        return sentenceRepository.findTtsWork(sentenceId, Status.PROCESSING)
                .map(sentence -> new TtsWork(
                        sentence.getId(),
                        sentence.getArticle().getId(),
                        sentence.getOrderIndex(),
                        sentence.getEnglishText()))
                .orElse(null);
    }

    @Transactional
    public void completeSuccess(Long sentenceId, String audioUrl) {
        sentenceRepository.findById(sentenceId).ifPresent(sentence -> {
            if (sentence.getTtsStatus() != Status.PROCESSING) {
                return;
            }
            sentence.setAudioUrl(audioUrl);
            sentence.setTtsStatus(Status.SUCCESS);
        });
    }

    @Transactional
    public TtsFailure completeFailure(Long sentenceId) {
        Sentence sentence = sentenceRepository.findById(sentenceId).orElse(null);
        if (sentence == null || sentence.getTtsStatus() != Status.PROCESSING) {
            return null;
        }

        sentence.incrementTtsRetryCount();
        sentence.setTtsStatus(Status.FAILED);
        return new TtsFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTtsRetryCount()
        );
    }

    @Transactional
    public TtsFailure recoverStale(Long sentenceId, LocalDateTime threshold, int maxRetryCount) {
        int recovered = sentenceRepository.recoverStaleTts(
                sentenceId,
                threshold,
                Status.FAILED,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        );
        if (recovered == 0) {
            return null;
        }

        Sentence sentence = sentenceRepository.findById(sentenceId).orElseThrow();
        return new TtsFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTtsRetryCount()
        );
    }

    @Transactional
    public int resetFinalFailure(Long articleId) {
        return sentenceRepository.resetFailedTtsRetryCounts(
                articleId,
                Status.FAILED,
                LocalDateTime.now()
        );
    }

    public boolean notifyIfExhausted(TtsFailure failure, int maxRetryCount, String reason) {
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

    public record TtsWork(Long sentenceId, Long articleId, Integer orderIndex, String englishText) {
    }

    public record TtsFailure(Long sentenceId, Long articleId, int retryCount) {
    }
}
