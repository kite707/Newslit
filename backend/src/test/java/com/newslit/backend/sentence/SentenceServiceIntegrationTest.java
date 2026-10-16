package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SentenceServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SentenceService sentenceService;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void setUp() {
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void 문장_분리는_번역을_실행하지_않고_멱등하게_문장만_생성한다() {
        Article article = articleRepository.save(Article.builder()
                .title("Sentence split test")
                .originalText("First sentence. Repeated sentence. Repeated sentence.")
                .publishedDate(LocalDate.of(2026, 10, 16))
                .source("TEST")
                .build());

        sentenceService.splitSentences(article.getId());
        sentenceService.splitSentences(article.getId());

        List<Sentence> sentences = sentenceRepository.findAllByArticleIdOrderByOrderIndexAsc(article.getId());
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();

        assertThat(sentences).hasSize(3);
        assertThat(sentences).extracting(Sentence::getOrderIndex).containsExactly(1, 2, 3);
        assertThat(sentences).extracting(Sentence::getEnglishText)
                .containsExactly("First sentence.", "Repeated sentence.", "Repeated sentence.");
        assertThat(sentences).allSatisfy(sentence -> {
            assertThat(sentence.getTranslationStatus()).isEqualTo(Status.PENDING);
            assertThat(sentence.getKoreanText()).isNull();
        });
        assertThat(updatedArticle.getPipelineStage()).isEqualTo(ArticlePipelineStage.TRANSLATION);
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.PENDING);
        assertThat(updatedArticle.getPipelineRetryCount()).isZero();
    }
}
