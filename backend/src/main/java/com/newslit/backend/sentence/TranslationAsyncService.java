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
        } catch (Exception e) {
            translationStateService.completeFailure(sentenceId);
            log.error("번역 처리 중 복구되지 않은 예외 - sentenceId: {}, articleId: {}",
                    sentenceId, work.articleId(), e);
        }
    }
}
