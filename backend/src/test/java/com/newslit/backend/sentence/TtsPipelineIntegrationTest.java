package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
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
        "pipeline.tts.batch-size=2",
        "pipeline.tts.max-retry-count=3",
        "pipeline.tts.fixed-delay-ms=3600000"
})
class TtsPipelineIntegrationTest extends IntegrationTestSupport {

    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private TtsBatchService ttsBatchService;

    @Autowired
    private TtsClaimService ttsClaimService;

    @Autowired
    private TtsStateService ttsStateService;

    @Autowired
    private TtsRecoveryService ttsRecoveryService;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @MockBean
    private TtsAsyncService ttsAsyncService;

    @MockBean
    private PipelineFailureEventPublisher failureEventPublisher;

    @BeforeEach
    void setUp() {
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void PENDING과_FAILED_문장을_배치_크기만큼만_선점한다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence pending = saveSentence(article, 1, Status.PENDING);
        Sentence failed = saveSentence(article, 2, Status.FAILED);
        Sentence waiting = saveSentence(article, 3, Status.PENDING);

        int claimedCount = ttsBatchService.processBatch();

        assertThat(claimedCount).isEqualTo(2);
        assertThat(ttsStatusOf(pending)).isEqualTo(Status.PROCESSING);
        assertThat(ttsStatusOf(failed)).isEqualTo(Status.PROCESSING);
        assertThat(ttsStatusOf(waiting)).isEqualTo(Status.PENDING);

        ArgumentCaptor<Long> sentenceIds = ArgumentCaptor.forClass(Long.class);
        verify(ttsAsyncService, times(2)).generateAsync(sentenceIds.capture());
        assertThat(sentenceIds.getAllValues()).containsExactly(pending.getId(), failed.getId());
    }

    @Test
    void 파이프라인이_끝나지_않은_Article의_문장은_자동_대상에서_제외한다() {
        Article pipelineRunning = articleRepository.save(article(LocalDate.of(2026, 10, 16))
                .pipelineStage(ArticlePipelineStage.DAILY_CHUNK)
                .pipelineStatus(Status.PENDING)
                .build());
        Sentence sentence = saveSentence(pipelineRunning, 1, Status.PENDING);

        int claimedCount = ttsBatchService.processBatch();

        assertThat(claimedCount).isZero();
        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PENDING);
        verify(ttsAsyncService, never()).generateAsync(anyLong());
    }

    @Test
    void 최신_기사의_문장을_먼저_선점한다() {
        Article older = saveCompletedArticle(LocalDate.of(2026, 9, 1));
        Sentence olderSentence = saveSentence(older, 1, Status.PENDING);
        Article newer = saveCompletedArticle(LocalDate.of(2026, 10, 17));
        Sentence newerFirst = saveSentence(newer, 1, Status.PENDING);
        Sentence newerSecond = saveSentence(newer, 2, Status.PENDING);

        int claimedCount = ttsBatchService.processBatch();

        assertThat(claimedCount).isEqualTo(2);
        assertThat(ttsStatusOf(newerFirst)).isEqualTo(Status.PROCESSING);
        assertThat(ttsStatusOf(newerSecond)).isEqualTo(Status.PROCESSING);
        assertThat(ttsStatusOf(olderSentence)).isEqualTo(Status.PENDING);
    }

    @Test
    void 같은_문장은_한_번만_선점된다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence sentence = saveSentence(article, 1, Status.PENDING);

        boolean firstClaim = ttsClaimService.claim(sentence.getId(), MAX_RETRY_COUNT);
        boolean secondClaim = ttsClaimService.claim(sentence.getId(), MAX_RETRY_COUNT);

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();
        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PROCESSING);
    }

    @Test
    void 동시에_선점해도_같은_문장은_한_작업만_가져간다() throws Exception {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence sentence = saveSentence(article, 1, Status.PENDING);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> claim = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return ttsClaimService.claim(sentence.getId(), MAX_RETRY_COUNT);
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

        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PROCESSING);
    }

    @Test
    void 재시도_한도에_도달한_문장은_배치_대상에서_제외한다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence exhausted = saveSentence(article, 1, Status.FAILED);
        exhausted.setTtsRetryCount(MAX_RETRY_COUNT);
        sentenceRepository.saveAndFlush(exhausted);

        int claimedCount = ttsBatchService.processBatch();

        assertThat(claimedCount).isZero();
        assertThat(ttsClaimService.claim(exhausted.getId(), MAX_RETRY_COUNT)).isFalse();
        verify(ttsAsyncService, never()).generateAsync(anyLong());
    }

    @Test
    void 오래된_PROCESSING은_FAILED로_복구하고_한도_도달_시_알림_이벤트를_발행한다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence stale = saveSentence(article, 1, Status.PROCESSING);
        stale.setTtsRetryCount(MAX_RETRY_COUNT - 1);
        sentenceRepository.saveAndFlush(stale);

        int recoveredCount = ttsRecoveryService
                .recoverStaleProcessing(LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isEqualTo(1);
        Sentence recovered = sentenceRepository.findById(stale.getId()).orElseThrow();
        assertThat(recovered.getTtsStatus()).isEqualTo(Status.FAILED);
        assertThat(recovered.getTtsRetryCount()).isEqualTo(MAX_RETRY_COUNT);
        verify(failureEventPublisher).publish(
                article.getId(),
                ArticlePipelineStage.TTS,
                MAX_RETRY_COUNT,
                "stale-processing sentenceId=" + stale.getId()
        );
    }

    @Test
    void TTS_최종_실패가_Article_파이프라인_상태를_되돌리지_않는다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence stale = saveSentence(article, 1, Status.PROCESSING);
        stale.setTtsRetryCount(MAX_RETRY_COUNT - 1);
        sentenceRepository.saveAndFlush(stale);

        ttsRecoveryService.recoverStaleProcessing(LocalDateTime.now().plusMinutes(1));

        Article unchanged = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(unchanged.getPipelineStage()).isEqualTo(ArticlePipelineStage.COMPLETED);
        assertThat(unchanged.getPipelineStatus()).isEqualTo(Status.SUCCESS);
        assertThat(unchanged.getPipelineRetryCount()).isZero();
    }

    @Test
    void 최종_실패한_문장은_자동_배치에서_제외되고_수동_재처리로만_복구된다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence exhausted = saveSentence(article, 1, Status.FAILED);
        exhausted.setTtsRetryCount(MAX_RETRY_COUNT);
        sentenceRepository.saveAndFlush(exhausted);

        assertThat(ttsBatchService.processBatch()).isZero();

        int claimedCount = ttsBatchService.processArticle(article.getId());

        assertThat(claimedCount).isEqualTo(1);
        Sentence recovered = sentenceRepository.findById(exhausted.getId()).orElseThrow();
        assertThat(recovered.getTtsRetryCount()).isZero();
        assertThat(recovered.getTtsStatus()).isEqualTo(Status.PROCESSING);
        verify(ttsAsyncService).generateAsync(exhausted.getId());
    }

    @Test
    void 수동_재처리는_파이프라인이_끝나지_않은_Article도_처리한다() {
        Article pipelineRunning = articleRepository.save(article(LocalDate.of(2026, 10, 16))
                .pipelineStage(ArticlePipelineStage.DAILY_CHUNK)
                .pipelineStatus(Status.PENDING)
                .build());
        Sentence sentence = saveSentence(pipelineRunning, 1, Status.PENDING);

        int claimedCount = ttsBatchService.processArticle(pipelineRunning.getId());

        assertThat(claimedCount).isEqualTo(1);
        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PROCESSING);
        verify(ttsAsyncService).generateAsync(sentence.getId());
    }

    @Test
    void 성공_처리는_오디오_URL을_저장하고_번역_상태는_건드리지_않는다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence sentence = sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Sentence 1.")
                .koreanText("문장 1.")
                .translationStatus(Status.SUCCESS)
                .translationRetryCount(2)
                .ttsStatus(Status.PROCESSING)
                .build());

        ttsStateService.completeSuccess(sentence.getId(), "https://storage/1.mp3");

        Sentence updated = sentenceRepository.findById(sentence.getId()).orElseThrow();
        assertThat(updated.getTtsStatus()).isEqualTo(Status.SUCCESS);
        assertThat(updated.getAudioUrl()).isEqualTo("https://storage/1.mp3");
        assertThat(updated.getTranslationStatus()).isEqualTo(Status.SUCCESS);
        assertThat(updated.getTranslationRetryCount()).isEqualTo(2);
    }

    @Test
    void 선점_상태가_아닌_문장은_성공으로_바꾸지_않는다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence sentence = saveSentence(article, 1, Status.PENDING);

        ttsStateService.completeSuccess(sentence.getId(), "https://storage/1.mp3");

        Sentence unchanged = sentenceRepository.findById(sentence.getId()).orElseThrow();
        assertThat(unchanged.getTtsStatus()).isEqualTo(Status.PENDING);
        assertThat(unchanged.getAudioUrl()).isNull();
    }

    @Test
    void TTS_실패는_번역_재시도_횟수를_건드리지_않는다() {
        Article article = saveCompletedArticle(LocalDate.of(2026, 10, 16));
        Sentence sentence = sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Sentence 1.")
                .translationStatus(Status.SUCCESS)
                .translationRetryCount(4)
                .ttsStatus(Status.PROCESSING)
                .build());

        TtsStateService.TtsFailure failure = ttsStateService.completeFailure(sentence.getId());

        assertThat(failure).isNotNull();
        assertThat(failure.retryCount()).isEqualTo(1);
        Sentence updated = sentenceRepository.findById(sentence.getId()).orElseThrow();
        assertThat(updated.getTtsStatus()).isEqualTo(Status.FAILED);
        assertThat(updated.getTtsRetryCount()).isEqualTo(1);
        assertThat(updated.getTranslationRetryCount()).isEqualTo(4);
        assertThat(updated.getTranslationStatus()).isEqualTo(Status.SUCCESS);
    }

    private Article saveCompletedArticle(LocalDate publishedDate) {
        return articleRepository.save(article(publishedDate)
                .pipelineStage(ArticlePipelineStage.COMPLETED)
                .pipelineStatus(Status.SUCCESS)
                .build());
    }

    private Article.ArticleBuilder article(LocalDate publishedDate) {
        return Article.builder()
                .title("TTS pipeline test " + System.nanoTime())
                .originalText("First sentence. Second sentence. Third sentence.")
                .publishedDate(publishedDate)
                .source("TEST");
    }

    private Sentence saveSentence(Article article, int orderIndex, Status ttsStatus) {
        return sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(orderIndex)
                .englishText("Sentence " + orderIndex + ".")
                .translationStatus(Status.SUCCESS)
                .ttsStatus(ttsStatus)
                .build());
    }

    private Status ttsStatusOf(Sentence sentence) {
        return sentenceRepository.findById(sentence.getId()).orElseThrow().getTtsStatus();
    }
}
