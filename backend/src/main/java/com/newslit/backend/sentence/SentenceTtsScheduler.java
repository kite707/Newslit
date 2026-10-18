package com.newslit.backend.sentence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceTtsScheduler {
    private final TtsBatchService ttsBatchService;
    private final TtsRecoveryService ttsRecoveryService;

    @Scheduled(
            initialDelayString = "${pipeline.tts.fixed-delay-ms:300000}",
            fixedDelayString = "${pipeline.tts.fixed-delay-ms:300000}")
    public void processTts() {
        int recoveredCount = ttsRecoveryService.recoverStaleProcessing();
        int claimedCount = ttsBatchService.processBatch();
        log.info("TTS 스케줄 실행 완료 - 복구: {}건, 선점: {}건", recoveredCount, claimedCount);
    }
}
