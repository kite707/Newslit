package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceScheduler {
    private static final int MAX_RETRY_COUNT = 10;
    private static final long RETRY_INTERVAL_MS = 300_000;

    private final SentenceService sentenceService;
    private final SentenceRepository sentenceRepository;

    @Scheduled(fixedDelay = RETRY_INTERVAL_MS)
    public void retryFailedTranslations() {
        List<Sentence> failedSentences = sentenceRepository
                .findAllByTranslationStatusAndRetryCountLessThan(Status.FAILED, MAX_RETRY_COUNT);

        log.info("재처리 대상: {}건", failedSentences.size());

        int successCount = 0;
        int failCount = 0;

        for (Sentence sentence : failedSentences) {
            sentence.incrementRetryCount();
            try {
                sentenceService.retryTranslation(sentence);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("재처리 중 예외 발생 - Sentence ID: {}", sentence.getId(), e);
            }
        }

        log.info("재처리 완료 - 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
