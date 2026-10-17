package com.newslit.backend.sentence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceScheduler {
    private final TranslationBatchService translationBatchService;
    private final TranslationRecoveryService translationRecoveryService;

    @Scheduled(
            initialDelayString = "${pipeline.translation.fixed-delay-ms:300000}",
            fixedDelayString = "${pipeline.translation.fixed-delay-ms:300000}")
    public void processTranslations() {
        int recoveredCount = translationRecoveryService.recoverStaleProcessing();
        int claimedCount = translationBatchService.processBatch();
        log.info("번역 스케줄 실행 완료 - 복구: {}건, 선점: {}건", recoveredCount, claimedCount);
    }
}
