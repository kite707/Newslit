package com.newslit.backend.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.daily.DailyPipelineService;
import com.newslit.backend.daily.DailyRespository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.sentence.SentenceSplitPipelineService;
import com.newslit.backend.sentence.TranslationBatchService;
import com.newslit.backend.sentence.TranslationRetryService;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "pipeline.sentence-split.batch-size=10",
        "pipeline.sentence-split.max-retry-count=3",
        "pipeline.sentence-split.fixed-delay-ms=3600000",
        "pipeline.translation.batch-size=10",
        "pipeline.translation.fixed-delay-ms=3600000",
        "pipeline.daily.batch-size=10",
        "pipeline.daily.fixed-delay-ms=3600000"
})
class ArticlePipelineIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SentenceSplitPipelineService sentenceSplitPipelineService;

    @Autowired
    private TranslationBatchService translationBatchService;

    @Autowired
    private DailyPipelineService dailyPipelineService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private DailyRespository dailyRespository;

    @MockBean
    private TranslationRetryService translationRetryService;

    @MockBean
    private PipelineFailureEventPublisher failureEventPublisher;

    @BeforeEach
    void setUp() throws Exception {
        dailyRespository.deleteAll();
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
        when(translationRetryService.translate(anyString()))
                .thenAnswer(invocation -> "번역: " + invocation.getArgument(0, String.class));
    }

    @Test
    void Oracle에서_크롤링_직후부터_Daily_완료까지_전체_파이프라인이_이어진다() {
        Article article = articleRepository.save(Article.builder()
                .title("full-pipeline-" + System.nanoTime())
                .originalText(articleText(4, 30))
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .build());

        assertThat(sentenceSplitPipelineService.processBatch()).isEqualTo(1);
        Article afterSplit = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(afterSplit.getPipelineStage()).isEqualTo(ArticlePipelineStage.TRANSLATION);
        assertThat(afterSplit.getPipelineStatus()).isEqualTo(Status.PENDING);
        assertThat(sentenceRepository.findAllByArticleId(article.getId())).hasSize(4);

        assertThat(translationBatchService.processBatch()).isEqualTo(4);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Article afterTranslation = articleRepository.findById(article.getId()).orElseThrow();
            assertThat(afterTranslation.getPipelineStage()).isEqualTo(ArticlePipelineStage.DAILY_CHUNK);
            assertThat(afterTranslation.getPipelineStatus()).isEqualTo(Status.PENDING);
        });

        assertThat(dailyPipelineService.processBatch()).isEqualTo(1);

        Article completed = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(completed.getPipelineStage()).isEqualTo(ArticlePipelineStage.COMPLETED);
        assertThat(completed.getPipelineStatus()).isEqualTo(Status.SUCCESS);
        assertThat(dailyRespository.findByArticleIdOrderByIdAsc(article.getId())).hasSize(1);
    }

    @Test
    void 문장_분리_재시도_한도에_도달하면_최종_실패_이벤트를_발행한다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-failure-" + System.nanoTime())
                .originalText("   ")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineRetryCount(2)
                .build());

        assertThat(sentenceSplitPipelineService.processBatch()).isEqualTo(1);

        Article failed = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(failed.getPipelineStage()).isEqualTo(ArticlePipelineStage.SENTENCE_SPLIT);
        assertThat(failed.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(failed.getPipelineRetryCount()).isEqualTo(3);
        verify(failureEventPublisher).publish(
                article.getId(),
                ArticlePipelineStage.SENTENCE_SPLIT,
                3,
                "분리된 문장이 없습니다"
        );
    }

    @Test
    void FAILED_문장_분리는_한도_미만이면_재시도한다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-retry-" + System.nanoTime())
                .originalText("First sentence. Second sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineStatus(Status.FAILED)
                .pipelineRetryCount(1)
                .build());

        assertThat(sentenceSplitPipelineService.processBatch()).isEqualTo(1);

        Article translated = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(translated.getPipelineStage()).isEqualTo(ArticlePipelineStage.TRANSLATION);
        assertThat(translated.getPipelineStatus()).isEqualTo(Status.PENDING);
        assertThat(translated.getPipelineRetryCount()).isZero();
        assertThat(sentenceRepository.findAllByArticleId(article.getId())).hasSize(2);
    }

    @Test
    void 오래된_문장_분리_PROCESSING이_한도에_도달하면_최종_FAILED로_복구한다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-stale-" + System.nanoTime())
                .originalText("First sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineStatus(Status.PROCESSING)
                .pipelineRetryCount(2)
                .build());

        int recoveredCount = sentenceSplitPipelineService
                .recoverStaleProcessing(LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isEqualTo(1);
        Article failed = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(failed.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(failed.getPipelineRetryCount()).isEqualTo(3);
        verify(failureEventPublisher).publish(
                article.getId(),
                ArticlePipelineStage.SENTENCE_SPLIT,
                3,
                "stale-processing"
        );
    }

    @Test
    void 기존_문장이_남아있어도_단계_전이에_실패하면_성공으로_처리하지_않는다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-no-advance-" + System.nanoTime())
                .originalText("   ")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .build());
        sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("Leftover sentence.")
                .translationStatus(Status.PENDING)
                .build());

        assertThat(sentenceSplitPipelineService.processBatch()).isEqualTo(1);

        Article notAdvanced = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(notAdvanced.getPipelineStage()).isEqualTo(ArticlePipelineStage.SENTENCE_SPLIT);
        assertThat(notAdvanced.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(notAdvanced.getPipelineRetryCount()).isEqualTo(1);
    }

    @Test
    void 수동_문장_분리는_이미_선점된_기사를_건드리지_않는다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-manual-claimed-" + System.nanoTime())
                .originalText("First sentence. Second sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineStatus(Status.PROCESSING)
                .build());

        assertThat(sentenceSplitPipelineService.processArticle(article.getId())).isFalse();

        assertThat(sentenceRepository.findAllByArticleId(article.getId())).isEmpty();
        Article untouched = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(untouched.getPipelineStage()).isEqualTo(ArticlePipelineStage.SENTENCE_SPLIT);
        assertThat(untouched.getPipelineStatus()).isEqualTo(Status.PROCESSING);
    }

    @Test
    void 수동_문장_분리는_선점에_성공하면_TRANSLATION으로_전이한다() {
        Article article = articleRepository.save(Article.builder()
                .title("split-manual-ok-" + System.nanoTime())
                .originalText("First sentence. Second sentence.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .build());

        assertThat(sentenceSplitPipelineService.processArticle(article.getId())).isTrue();

        Article advanced = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(advanced.getPipelineStage()).isEqualTo(ArticlePipelineStage.TRANSLATION);
        assertThat(advanced.getPipelineStatus()).isEqualTo(Status.PENDING);
        assertThat(sentenceRepository.findAllByArticleId(article.getId())).hasSize(2);
    }

    @Test
    void 수동_문장_분리는_없는_기사면_예외를_던진다() {
        assertThatThrownBy(() -> sentenceSplitPipelineService.processArticle(-1L))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    private String articleText(int sentenceCount, int wordsPerSentence) {
        String sentence = "Word " + "word ".repeat(wordsPerSentence - 2) + "word.";
        return (sentence + " ").repeat(sentenceCount).trim();
    }
}
