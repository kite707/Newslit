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
        Article article = saveCompletedArticle();
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
        Article pipelineRunning = articleRepository.save(article()
                .pipelineStage(ArticlePipelineStage.DAILY_CHUNK)
                .pipelineStatus(Status.PENDING)
                .build());
        Sentence sentence = saveSentence(pipelineRunning, 1, Status.PENDING);

        assertThat(ttsBatchService.processBatch()).isZero();
        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PENDING);
        verify(ttsAsyncService, never()).generateAsync(anyLong());
    }

    @Test
    void 같은_문장은_한_번만_선점된다() {
        Article article = saveCompletedArticle();
        Sentence sentence = saveSentence(article, 1, Status.PENDING);

        assertThat(ttsBatchService.claim(sentence.getId())).isTrue();
        assertThat(ttsBatchService.claim(sentence.getId())).isFalse();
        assertThat(ttsStatusOf(sentence)).isEqualTo(Status.PROCESSING);
    }

    @Test
    void 오래된_PROCESSING을_복구하면서_한도_도달_시_알리고_Article_상태는_유지한다() {
        Article article = saveCompletedArticle();
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

        Article unchanged = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(unchanged.getPipelineStage()).isEqualTo(ArticlePipelineStage.COMPLETED);
        assertThat(unchanged.getPipelineStatus()).isEqualTo(Status.SUCCESS);
    }

    @Test
    void 한도를_소진한_문장은_자동_배치에서_제외되고_수동_재처리로만_복구된다() {
        Article article = saveCompletedArticle();
        Sentence exhausted = saveSentence(article, 1, Status.FAILED);
        exhausted.setTtsRetryCount(MAX_RETRY_COUNT);
        sentenceRepository.saveAndFlush(exhausted);

        assertThat(ttsBatchService.processBatch()).isZero();
        verify(ttsAsyncService, never()).generateAsync(anyLong());

        assertThat(ttsBatchService.processArticle(article.getId())).isEqualTo(1);
        Sentence recovered = sentenceRepository.findById(exhausted.getId()).orElseThrow();
        assertThat(recovered.getTtsRetryCount()).isZero();
        assertThat(recovered.getTtsStatus()).isEqualTo(Status.PROCESSING);
        verify(ttsAsyncService).generateAsync(exhausted.getId());
    }

    @Test
    void 성공_처리는_오디오_URL을_저장하고_번역_상태는_건드리지_않는다() {
        Article article = saveCompletedArticle();
        Sentence sentence = sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Sentence 1.")
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

    private Article saveCompletedArticle() {
        return articleRepository.save(article()
                .pipelineStage(ArticlePipelineStage.COMPLETED)
                .pipelineStatus(Status.SUCCESS)
                .build());
    }

    private Article.ArticleBuilder article() {
        return Article.builder()
                .title("TTS pipeline test " + System.nanoTime())
                .originalText("First sentence. Second sentence. Third sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
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
