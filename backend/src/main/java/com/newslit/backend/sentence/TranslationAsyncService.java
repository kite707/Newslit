package com.newslit.backend.sentence;

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
    private final TranslationRetryService translationRetryService;

    @Async("externalApiExecutor")
    public void translateAsync(Sentence sentence, String englishText, Long articleId) {
        sentence.setTranslationStatus(Status.PROCESSING);
        sentenceRepository.save(sentence);

        try {
            translationRetryService.translate(sentence, englishText, articleId);
        } catch (Exception e) {
            log.error("번역 처리 중 복구되지 않은 예외 - sentenceId: {}, articleId: {}",
                    sentence.getId(), articleId, e);
        }
    }
}
