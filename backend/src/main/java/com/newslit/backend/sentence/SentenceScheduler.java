package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceScheduler {
    private final SentenceRepository sentenceRepository;
    private final TranslationBatchService translationBatchService;
    private final TranslationProperties properties;

    @Scheduled(
            initialDelayString = "${pipeline.translation.fixed-delay-ms:300000}",
            fixedDelayString = "${pipeline.translation.fixed-delay-ms:300000}")
    public void processTranslations() {
        int recoveredCount = sentenceRepository.markStaleProcessingAsFailed(
                LocalDateTime.now().minusMinutes(properties.getStaleProcessingMinutes()),
                Status.FAILED,
                Status.PROCESSING);

        if (recoveredCount > 0) {
            log.warn("{}분 넘게 PROCESSING인 문장 {}건을 FAILED로 복구",
                    properties.getStaleProcessingMinutes(), recoveredCount);
        }

        int claimedCount = translationBatchService.processBatch();
        log.info("번역 스케줄 실행 완료 - 선점: {}건", claimedCount);
    }
}
