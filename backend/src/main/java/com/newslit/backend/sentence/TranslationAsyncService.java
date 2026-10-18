package com.newslit.backend.sentence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAsyncService {

    private final TranslationRetryService translationRetryService;
    private final TranslationStateService translationStateService;
    private final TranslationFailureService translationFailureService;
    private final TranslationProperties properties;

    @Async("externalApiExecutor")
    public void translateAsync(Long sentenceId) {
        TranslationStateService.TranslationWork work = translationStateService.getWork(sentenceId);
        if (work == null) {
            log.info("선점 상태가 아닌 번역 작업 스킵 - sentenceId: {}", sentenceId);
            return;
        }

        try {
            String translatedText = translationRetryService.translate(work.englishText());
            translationStateService.completeSuccess(sentenceId, translatedText);
            translationStateService.advanceArticleIfComplete(work.articleId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            TranslationStateService.TranslationFailure failure = handleFailure(sentenceId, "interrupted");
            logFailure(work, failure, "interrupted", e);
        } catch (Exception e) {
            String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
            TranslationStateService.TranslationFailure failure = handleFailure(sentenceId, reason);
            logFailure(work, failure, reason, e);
        }
    }

    private TranslationStateService.TranslationFailure handleFailure(Long sentenceId, String reason) {
        TranslationStateService.TranslationFailure failure = translationStateService.completeFailure(sentenceId);
        translationFailureService.finalizeIfExhausted(
                failure,
                properties.getMaxRetryCount(),
                reason
        );
        return failure;
    }

    private void logFailure(
            TranslationStateService.TranslationWork work,
            TranslationStateService.TranslationFailure failure,
            String reason,
            Exception exception
    ) {
        int retryCount = failure == null ? -1 : failure.retryCount();
        log.error(
                "pipelineFailure=true articleId={} sentenceId={} stage=TRANSLATION retryCount={} reason={}",
                work.articleId(),
                work.sentenceId(),
                retryCount,
                reason,
                exception
        );
    }
}
