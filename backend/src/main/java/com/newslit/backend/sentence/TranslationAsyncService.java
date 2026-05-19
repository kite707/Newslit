package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.newslit.backend.global.common.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationAsyncService {
    private final SentenceRepository sentenceRepository;
    private final DeepLClient deepLClient;
    private static final int MAX_RETRY_CNT = 3;

    @Async("externalApiExecutor")
    public void translateAsync(
            Sentence sentence, String englishText, Long articleId, int orderIndex) {
        sentence.setTranslationStatus(Status.PROCESSING);
        sentenceRepository.save(sentence);

        for (int retryCnt = 0; retryCnt < MAX_RETRY_CNT; retryCnt++) {
            try {
                String translatedText = deepLClient.translateText(englishText, null, "ko").getText();

                sentence.setKoreanText(translatedText);
                sentence.setTranslationStatus(Status.SUCCESS);
                sentenceRepository.save(sentence);
                return;
            } catch (Exception e) {
                log.warn("번역 실패 (재시도 {}/{}): {}", retryCnt + 1, MAX_RETRY_CNT, e.getMessage());
            }
        }
        sentence.setTranslationStatus(Status.FAILED);
        sentenceRepository.save(sentence);
        log.error("번역 최종 실패 - sentenceId: {}, articleId: {}", sentence.getId(), articleId);
    }
}