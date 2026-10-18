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
    private static final List<Status> ACTIVE_PIPELINE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);

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
    public TranslationFailure completeFailure(Long sentenceId) {
        Sentence sentence = sentenceRepository.findById(sentenceId).orElse(null);
        if (sentence == null || sentence.getTranslationStatus() != Status.PROCESSING) {
            return null;
        }

        sentence.incrementTranslationRetryCount();
        sentence.setTranslationStatus(Status.FAILED);
        return new TranslationFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTranslationRetryCount()
        );
    }

    @Transactional
    public TranslationFailure recoverStale(Long sentenceId, LocalDateTime threshold, int maxRetryCount) {
        int recovered = sentenceRepository.recoverStaleTranslation(
                sentenceId,
                ArticlePipelineStage.TRANSLATION,
                ACTIVE_PIPELINE_STATUSES,
                threshold,
                Status.FAILED,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        );
        if (recovered == 0) {
            return null;
        }

        Sentence sentence = sentenceRepository.findById(sentenceId).orElseThrow();
        return new TranslationFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTranslationRetryCount()
        );
    }

    /**
     * 재시도 한도를 모두 소진해 FAILED로 멈춘 번역 단계를 다시 PENDING으로 되돌린다.
     * 운영자가 수동 API로 호출하는 복구 경로이며, 진행 중(PROCESSING)인 문장은 건드리지 않고
     * stale 복구에 맡긴다.
     */
    @Transactional
    public boolean resetFinalFailure(Long articleId) {
        LocalDateTime resetAt = LocalDateTime.now();
        int reset = articleRepository.resetFailedTranslationPipeline(
                articleId,
                ArticlePipelineStage.TRANSLATION,
                Status.FAILED,
                Status.PENDING,
                resetAt
        );
        if (reset == 0) {
            return false;
        }

        sentenceRepository.resetFailedTranslationRetryCounts(articleId, Status.FAILED, resetAt);
        return true;
    }

    @Transactional
    public boolean advanceArticleIfComplete(Long articleId) {
        if (sentenceRepository.countByArticleId(articleId) == 0) {
            return false;
        }
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

    public record TranslationFailure(Long sentenceId, Long articleId, int retryCount) {
    }
}
