package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TranslationStateService {
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public TranslationWork getWork(Long sentenceId) {
        return sentenceRepository.findTranslationWork(sentenceId, Status.PROCESSING)
                .map(sentence -> new TranslationWork(
                        sentence.getId(),
                        sentence.getArticle().getId(),
                        sentence.getEnglishText()))
                .orElse(null);
    }

    @Transactional
    public void completeSuccess(Long sentenceId, String translatedText) {
        sentenceRepository.findById(sentenceId).ifPresent(sentence -> {
            if (sentence.getTranslationStatus() != Status.PROCESSING) {
                return;
            }
            sentence.setKoreanText(translatedText);
            sentence.setTranslationStatus(Status.SUCCESS);
        });
    }

    @Transactional
    public void completeFailure(Long sentenceId) {
        sentenceRepository.findById(sentenceId).ifPresent(sentence -> {
            if (sentence.getTranslationStatus() != Status.PROCESSING) {
                return;
            }
            sentence.incrementTranslationRetryCount();
            sentence.setTranslationStatus(Status.FAILED);
        });
    }

    @Transactional
    public boolean advanceArticleIfComplete(Long articleId) {
        if (sentenceRepository.countByArticleIdAndTranslationStatusNot(articleId, Status.SUCCESS) > 0) {
            return false;
        }

        return articleRepository.advanceTranslationToDailyChunk(
                articleId,
                ArticlePipelineStage.TRANSLATION,
                ArticlePipelineStage.DAILY_CHUNK,
                List.of(Status.PENDING, Status.PROCESSING),
                Status.PENDING,
                LocalDateTime.now()
        ) == 1;
    }

    public record TranslationWork(Long sentenceId, Long articleId, String englishText) {
    }
}
