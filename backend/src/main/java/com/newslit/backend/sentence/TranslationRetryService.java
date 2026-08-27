package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.newslit.backend.global.common.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationRetryService {

    static final int MAX_ATTEMPTS = 3;

    private final SentenceRepository sentenceRepository;
    private final DeepLClient deepLClient;

    @Retryable(
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = 500, multiplier = 2, maxDelay = 2000, random = true))
    public void translate(Sentence sentence, String englishText, Long articleId)
            throws DeepLException, InterruptedException {
        String translatedText = deepLClient.translateText(englishText, null, "ko").getText();

        sentence.setKoreanText(translatedText);
        sentence.setTranslationStatus(Status.SUCCESS);
        sentenceRepository.save(sentence);
    }

    @Recover
    public void recover(Exception e, Sentence sentence, String englishText, Long articleId) {
        sentence.setTranslationStatus(Status.FAILED);
        sentenceRepository.save(sentence);
        log.error("번역 최종 실패({}회 시도) - sentenceId: {}, articleId: {}",
                MAX_ATTEMPTS, sentence.getId(), articleId, e);
    }
}
