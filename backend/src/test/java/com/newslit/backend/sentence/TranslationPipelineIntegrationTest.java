package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "pipeline.translation.batch-size=2",
        "pipeline.translation.max-retry-count=3",
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
    private TranslationRecoveryService translationRecoveryService;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @MockBean
    private TranslationAsyncService translationAsyncService;

    @MockBean
    private PipelineFailureEventPublisher failureEventPublisher;

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
    void 동시에_선점해도_같은_문장은_한_작업만_가져간다() throws Exception {
        Article article = saveTranslationArticle();
        Sentence sentence = saveSentence(article, 1, Status.PENDING);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> claim = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return translationClaimService.claim(sentence.getId(), article.getId(), 3);
        };

        try {
            Future<Boolean> first = executor.submit(claim);
            Future<Boolean> second = executor.submit(claim);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

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
    void 마지막_번역_성공_후_상태_전이가_누락돼도_다음_배치에서_복구한다() {
        Article article = saveTranslationArticle();
        article.setPipelineStatus(Status.PROCESSING);
        articleRepository.save(article);
        saveSentence(article, 1, Status.SUCCESS);
        saveSentence(article, 2, Status.SUCCESS);

        int claimedCount = translationBatchService.processBatch();

        assertThat(claimedCount).isZero();
        Article reconciled = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(reconciled.getPipelineStage()).isEqualTo(ArticlePipelineStage.DAILY_CHUNK);
        assertThat(reconciled.getPipelineStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    void 문장이_없는_Article은_번역_완료로_간주하지_않는다() {
        Article article = saveTranslationArticle();

        assertThat(translationStateService.advanceArticleIfComplete(article.getId())).isFalse();
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getPipelineStage())
                .isEqualTo(ArticlePipelineStage.TRANSLATION);
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

    @Test
    void 오래된_PROCESSING이_재시도_한도에_도달하면_Article을_최종_FAILED로_처리한다() {
        Article article = saveTranslationArticle();
        article.setPipelineStatus(Status.PROCESSING);
        articleRepository.save(article);
        Sentence sentence = sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Stale sentence.")
                .translationStatus(Status.PROCESSING)
                .translationRetryCount(2)
                .build());

        int recoveredCount = translationRecoveryService
                .recoverStaleProcessing(LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isEqualTo(1);
        Sentence updatedSentence = sentenceRepository.findById(sentence.getId()).orElseThrow();
        assertThat(updatedSentence.getTranslationStatus()).isEqualTo(Status.FAILED);
        assertThat(updatedSentence.getTranslationRetryCount()).isEqualTo(3);
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.FAILED);
        verify(failureEventPublisher).publish(
                article.getId(),
                ArticlePipelineStage.TRANSLATION,
                3,
                "stale-processing sentenceId=" + sentence.getId()
        );
    }

    @Test
    void 재시도_한도에_도달한_번역은_배치_대상에서_제외한다() {
        Article article = saveTranslationArticle();
        article.setPipelineStatus(Status.PROCESSING);
        articleRepository.save(article);
        Sentence exhausted = saveSentence(article, 1, Status.FAILED);
        exhausted.setTranslationRetryCount(3);
        sentenceRepository.saveAndFlush(exhausted);

        int claimedCount = translationBatchService.processBatch();

        assertThat(claimedCount).isZero();
        verify(translationAsyncService, times(0)).translateAsync(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void 최종_실패한_번역은_자동_배치에서_제외되고_수동_재처리로만_복구된다() {
        Article article = saveTranslationArticle();
        article.setPipelineStatus(Status.FAILED);
        article.setPipelineRetryCount(1);
        articleRepository.save(article);
        Sentence exhausted = saveSentence(article, 1, Status.FAILED);
        exhausted.setTranslationRetryCount(3);
        sentenceRepository.saveAndFlush(exhausted);

        assertThat(translationBatchService.processBatch()).isZero();

        int claimedCount = translationBatchService.processArticle(article.getId());

        assertThat(claimedCount).isEqualTo(1);
        Sentence recovered = sentenceRepository.findById(exhausted.getId()).orElseThrow();
        assertThat(recovered.getTranslationRetryCount()).isZero();
        assertThat(recovered.getTranslationStatus()).isEqualTo(Status.PROCESSING);
        Article reopened = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(reopened.getPipelineStage()).isEqualTo(ArticlePipelineStage.TRANSLATION);
        assertThat(reopened.getPipelineStatus()).isEqualTo(Status.PROCESSING);
        verify(translationAsyncService).translateAsync(exhausted.getId());
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
