package com.newslit.backend.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.daily.Daily;
import com.newslit.backend.daily.DailyRespository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArticlePipelineBackfillIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ArticlePipelineBackfillService backfillService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private DailyRespository dailyRespository;

    @BeforeEach
    void setUp() {
        dailyRespository.deleteAll();
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void 기존_Article을_생성된_데이터에_맞는_파이프라인_단계로_보정한다() {
        Article split = saveLegacyArticle("legacy-split");
        Article translation = saveLegacyArticle("legacy-translation");
        Sentence legacyFailedSentence = saveSentence(translation, Status.FAILED);
        legacyFailedSentence.setTranslationRetryCount(99);
        sentenceRepository.save(legacyFailedSentence);
        Article dailyChunk = saveLegacyArticle("legacy-daily");
        saveSentence(dailyChunk, Status.SUCCESS);
        Article completed = saveLegacyArticle("legacy-completed");
        saveSentence(completed, Status.SUCCESS);
        dailyRespository.save(Daily.builder()
                .article(completed)
                .displayDate(LocalDate.of(2026, 10, 17))
                .startIndex(1)
                .endIndex(1)
                .wordCount(10)
                .build());

        ArticlePipelineBackfillService.BackfillResult result = backfillService.backfillLegacyArticles();

        assertThat(result.translation()).isEqualTo(1);
        assertThat(result.dailyChunk()).isEqualTo(1);
        assertThat(result.completed()).isEqualTo(1);
        assertPipeline(split, ArticlePipelineStage.SENTENCE_SPLIT, Status.PENDING);
        assertPipeline(translation, ArticlePipelineStage.TRANSLATION, Status.PENDING);
        assertThat(sentenceRepository.findById(legacyFailedSentence.getId()).orElseThrow()
                .getTranslationRetryCount()).isZero();
        assertPipeline(dailyChunk, ArticlePipelineStage.DAILY_CHUNK, Status.PENDING);
        assertPipeline(completed, ArticlePipelineStage.COMPLETED, Status.SUCCESS);
    }

    @Test
    void 기존_Article_보정은_재실행해도_상태를_다시_변경하지_않는다() {
        Article article = saveLegacyArticle("legacy-idempotent");
        saveSentence(article, Status.SUCCESS);

        assertThat(backfillService.backfillLegacyArticles().total()).isEqualTo(1);
        assertThat(backfillService.backfillLegacyArticles().total()).isZero();
        assertPipeline(article, ArticlePipelineStage.DAILY_CHUNK, Status.PENDING);
    }

    private Article saveLegacyArticle(String title) {
        return articleRepository.save(Article.builder()
                .title(title + "-" + System.nanoTime())
                .originalText("Legacy article.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .build());
    }

    private Sentence saveSentence(Article article, Status translationStatus) {
        return sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Legacy sentence.")
                .koreanText(translationStatus == Status.SUCCESS ? "기존 문장" : null)
                .translationStatus(translationStatus)
                .build());
    }

    private void assertPipeline(Article article, ArticlePipelineStage stage, Status status) {
        Article updated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getPipelineStage()).isEqualTo(stage);
        assertThat(updated.getPipelineStatus()).isEqualTo(status);
        assertThat(updated.getPipelineRetryCount()).isZero();
    }
}
