package com.newslit.backend.daily;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyScheduler {
    private final DailyPipelineService dailyPipelineService;

    @Scheduled(
            initialDelayString = "${pipeline.daily.fixed-delay-ms:300000}",
            fixedDelayString = "${pipeline.daily.fixed-delay-ms:300000}")
    public void createDailyChunks() {
        int claimedCount = dailyPipelineService.processBatch();
        log.info("Daily 스케줄 실행 완료 - 선점: {}건", claimedCount);
    }
}
