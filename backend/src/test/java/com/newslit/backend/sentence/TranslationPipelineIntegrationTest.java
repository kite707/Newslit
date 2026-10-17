package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "pipeline.translation.batch-size=2",
        "pipeline.translation.fixed-delay-ms=3600000"
})
class TranslationPipelineIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TranslationBatchService translationBatchService;

    @Autowired
    private TranslationClaimService translationClaimService;

    @Autowired
    private TranslationStateService translationStateService;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @MockBean
    private TranslationAsyncService translationAsyncService;

    @BeforeEach
    void setUp() {
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void PENDING과_FAILED_문장을_배치_크기만큼만_선점한다() {
        Article article = saveTranslationArticle();
        Sentence pending = saveSentence(article, 1, Status.PENDING);
        Sentence failed = saveSentence(article, 2, Status.FAILED);
        Sentence waiting = saveSentence(article, 3, Status.PENDING);

        int claimedCount = translationBatchService.processBatch();

        assertThat(claimedCount).isEqualTo(2);
        assertThat(statusOf(pending)).isEqualTo(Status.PROCESSING);
        assertThat(statusOf(failed)).isEqualTo(Status.PROCESSING);
        assertThat(statusOf(waiting)).isEqualTo(Status.PENDING);
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getPipelineStatus())
                .isEqualTo(Status.PROCESSING);

        ArgumentCaptor<Long> sentenceIds = ArgumentCaptor.forClass(Long.class);
        verify(translationAsyncService, times(2)).translateAsync(sentenceIds.capture());
        assertThat(sentenceIds.getAllValues()).containsExactly(pending.getId(), failed.getId());
    }

    @Test
    void 같은_문장은_한_번만_선점된다() {
        Article article = saveTranslationArticle();
        Sentence sentence = saveSentence(article, 1, Status.PENDING);

        boolean firstClaim = translationClaimService.claim(sentence.getId(), article.getId(), 10);
        boolean secondClaim = translationClaimService.claim(sentence.getId(), article.getId(), 10);

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
        assertThat(statusOf(sentence)).isEqualTo(Status.PROCESSING);
    }

    @Test
    void 모든_문장_번역이_성공하면_DAILY_CHUNK_PENDING으로_전이한다() {
        Article article = saveTranslationArticle();
        article.setPipelineStatus(Status.PROCESSING);
        articleRepository.save(article);
        Sentence first = saveSentence(article, 1, Status.PROCESSING);
        Sentence second = saveSentence(article, 2, Status.PROCESSING);

        translationStateService.completeSuccess(first.getId(), "첫 문장");
        assertThat(translationStateService.advanceArticleIfComplete(article.getId())).isFalse();

        translationStateService.completeSuccess(second.getId(), "둘째 문장");
        assertThat(translationStateService.advanceArticleIfComplete(article.getId())).isTrue();

        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStage()).isEqualTo(ArticlePipelineStage.DAILY_CHUNK);
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.PENDING);
        assertThat(updatedArticle.getPipelineRetryCount()).isZero();
    }

    @Test
    void 번역_재시도_횟수는_TTS_재시도_횟수와_독립적이다() {
        Article article = saveTranslationArticle();
        Sentence sentence = sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Failed sentence.")
                .translationStatus(Status.PROCESSING)
                .ttsRetryCount(4)
                .build());

        translationStateService.completeFailure(sentence.getId());

        Sentence updatedSentence = sentenceRepository.findById(sentence.getId()).orElseThrow();
        assertThat(updatedSentence.getTranslationRetryCount()).isEqualTo(1);
        assertThat(updatedSentence.getTtsRetryCount()).isEqualTo(4);
        assertThat(updatedSentence.getTranslationStatus()).isEqualTo(Status.FAILED);
    }

    private Article saveTranslationArticle() {
        return articleRepository.save(Article.builder()
                .title("Translation pipeline test " + System.nanoTime())
                .originalText("First sentence. Second sentence. Third sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineStage(ArticlePipelineStage.TRANSLATION)
                .pipelineStatus(Status.PENDING)
                .build());
    }

    private Sentence saveSentence(Article article, int orderIndex, Status status) {
        return sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(orderIndex)
                .englishText("Sentence " + orderIndex + ".")
                .translationStatus(status)
                .build());
    }

    private Status statusOf(Sentence sentence) {
        return sentenceRepository.findById(sentence.getId()).orElseThrow().getTranslationStatus();
    }
}
