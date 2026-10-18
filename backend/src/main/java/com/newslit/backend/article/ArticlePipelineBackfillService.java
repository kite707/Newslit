package com.newslit.backend.article;

import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.SentenceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticlePipelineBackfillService {

    private final ArticleRepository articleRepository;
    private final SentenceRepository sentenceRepository;

    @Transactional
    public BackfillResult backfillLegacyArticles() {
        LocalDateTime updatedAt = LocalDateTime.now();
        int completed = articleRepository.backfillLegacyCompletedArticles(
                ArticlePipelineStage.SENTENCE_SPLIT,
                ArticlePipelineStage.COMPLETED,
                Status.PENDING,
                Status.SUCCESS,
                updatedAt
        );
        int dailyChunk = articleRepository.backfillLegacyDailyChunkArticles(
                ArticlePipelineStage.SENTENCE_SPLIT,
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PENDING,
                Status.SUCCESS,
                updatedAt
        );
        sentenceRepository.resetLegacyTranslationRetryCounts(
                ArticlePipelineStage.SENTENCE_SPLIT,
                Status.PENDING,
                Status.SUCCESS
        );
        int translation = articleRepository.backfillLegacyTranslationArticles(
                ArticlePipelineStage.SENTENCE_SPLIT,
                ArticlePipelineStage.TRANSLATION,
                Status.PENDING,
                updatedAt
        );
        return new BackfillResult(translation, dailyChunk, completed);
    }

    public record BackfillResult(int translation, int dailyChunk, int completed) {
        public int total() {
            return translation + dailyChunk + completed;
        }
    }
}
