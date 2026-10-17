package com.newslit.backend.sentence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceSplitScheduler {
    private final SentenceSplitPipelineService pipelineService;

    @Scheduled(
            initialDelayString = "${pipeline.sentence-split.fixed-delay-ms:300000}",
            fixedDelayString = "${pipeline.sentence-split.fixed-delay-ms:300000}")
    public void splitSentences() {
        int recoveredCount = pipelineService.recoverStaleProcessing();
        int claimedCount = pipelineService.processBatch();
        log.info("문장 분리 스케줄 실행 완료 - 복구: {}건, 선점: {}건", recoveredCount, claimedCount);
    }
}
