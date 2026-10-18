package com.newslit.backend.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.global.pipeline.PipelineFailureEventPublisher;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "pipeline.daily.batch-size=2",
        "pipeline.daily.max-retry-count=3",
        "pipeline.daily.fixed-delay-ms=3600000"
})
class DailyPipelineIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DailyPipelineService dailyPipelineService;

    @Autowired
    private DailyPipelineStateService dailyPipelineStateService;

    @Autowired
    private DailyService dailyService;

    @Autowired
    private DailyRespository dailyRespository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @MockBean
    private PipelineFailureEventPublisher failureEventPublisher;

    @BeforeEach
    void setUp() {
        dailyRespository.deleteAll();
        sentenceRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void DAILY_CHUNK_PENDING_기사를_자동_생성하고_COMPLETED_SUCCESS로_전이한다() {
        Article article = saveDailyChunkArticle("daily-success");
        IntStream.rangeClosed(1, 8)
                .forEach(index -> saveSentence(article, index, Status.SUCCESS, 30));

        int claimedCount = dailyPipelineService.processBatch();

        assertThat(claimedCount).isEqualTo(1);
        assertThat(dailyRespository.findByArticleIdOrderByIdAsc(article.getId())).hasSize(2);
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStage()).isEqualTo(ArticlePipelineStage.COMPLETED);
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.SUCCESS);
        assertThat(updatedArticle.getPipelineRetryCount()).isZero();
    }

    @Test
    void 번역이_완료되지_않으면_Daily를_만들지_않고_FAILED로_처리한다() {
        Article article = saveDailyChunkArticle("daily-untranslated");
        saveSentence(article, 1, Status.SUCCESS, 30);
        saveSentence(article, 2, Status.PENDING, 30);

        dailyPipelineService.processBatch();

        assertThat(dailyRespository.findByArticleIdOrderByIdAsc(article.getId())).isEmpty();
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStage()).isEqualTo(ArticlePipelineStage.DAILY_CHUNK);
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(updatedArticle.getPipelineRetryCount()).isEqualTo(1);
    }

    @Test
    void 문장이_없으면_Daily를_만들지_않고_FAILED로_처리한다() {
        Article article = saveDailyChunkArticle("daily-empty");

        dailyPipelineService.processBatch();

        assertThat(dailyRespository.findByArticleIdOrderByIdAsc(article.getId())).isEmpty();
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(updatedArticle.getPipelineRetryCount()).isEqualTo(1);
    }

    @Test
    void 단어_수를_계산할_수_없으면_Daily를_만들지_않고_FAILED로_처리한다() {
        Article article = saveDailyChunkArticle("daily-no-words");
        sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(1)
                .englishText("   ")
                .koreanText("번역 문장")
                .translationStatus(Status.SUCCESS)
                .build());

        dailyPipelineService.processBatch();

        assertThat(dailyRespository.findByArticleIdOrderByIdAsc(article.getId())).isEmpty();
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(updatedArticle.getPipelineRetryCount()).isEqualTo(1);
    }

    @Test
    void 같은_기사의_Daily를_다시_생성해도_중복되지_않는다() {
        Article article = saveDailyChunkArticle("daily-idempotent");
        IntStream.rangeClosed(1, 8)
                .forEach(index -> saveSentence(article, index, Status.SUCCESS, 30));

        dailyService.createChunks(article.getId());
        List<Daily> firstResult = dailyRespository.findByArticleIdOrderByIdAsc(article.getId());
        dailyService.createChunks(article.getId());
        List<Daily> secondResult = dailyRespository.findByArticleIdOrderByIdAsc(article.getId());

        assertThat(firstResult).hasSize(2);
        assertThat(secondResult).hasSize(2);
        assertThat(secondResult)
                .extracting(Daily::getId)
                .containsExactlyElementsOf(firstResult.stream().map(Daily::getId).toList());
    }

    @Test
    void 같은_기사는_한_번만_선점된다() {
        Article article = saveDailyChunkArticle("daily-claim");

        assertThat(dailyPipelineStateService.claim(article.getId(), 3)).isTrue();
        assertThat(dailyPipelineStateService.claim(article.getId(), 3)).isFalse();
    }

    @Test
    void FAILED_Daily는_재시도_한도_미만이면_다시_처리한다() {
        Article article = saveDailyChunkArticle("daily-retry");
        article.setPipelineStatus(Status.FAILED);
        article.setPipelineRetryCount(1);
        articleRepository.save(article);
        IntStream.rangeClosed(1, 4)
                .forEach(index -> saveSentence(article, index, Status.SUCCESS, 30));

        int claimedCount = dailyPipelineService.processBatch();

        assertThat(claimedCount).isEqualTo(1);
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStage()).isEqualTo(ArticlePipelineStage.COMPLETED);
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.SUCCESS);
        assertThat(updatedArticle.getPipelineRetryCount()).isZero();
    }

    @Test
    void 재시도_한도에_도달한_Daily는_배치_대상에서_제외한다() {
        Article article = saveDailyChunkArticle("daily-exhausted");
        article.setPipelineStatus(Status.FAILED);
        article.setPipelineRetryCount(3);
        articleRepository.save(article);

        int claimedCount = dailyPipelineService.processBatch();

        assertThat(claimedCount).isZero();
        assertThat(articleRepository.findById(article.getId()).orElseThrow().getPipelineStatus())
                .isEqualTo(Status.FAILED);
    }

    @Test
    void 오래된_PROCESSING_Daily가_한도에_도달하면_최종_실패_이벤트를_발행한다() {
        Article article = saveDailyChunkArticle("daily-stale");
        article.setPipelineStatus(Status.PROCESSING);
        article.setPipelineRetryCount(2);
        articleRepository.save(article);

        int recoveredCount = dailyPipelineService
                .recoverStaleProcessing(LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isEqualTo(1);
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(updatedArticle.getPipelineStatus()).isEqualTo(Status.FAILED);
        assertThat(updatedArticle.getPipelineRetryCount()).isEqualTo(3);
        verify(failureEventPublisher).publish(
                article.getId(),
                ArticlePipelineStage.DAILY_CHUNK,
                3,
                "stale-processing"
        );
    }

    @Test
    void 서로_다른_기사의_Daily가_같은_날짜를_쓰면_DB가_거부한다() {
        Article first = saveDailyChunkArticle("daily-date-a");
        Article second = saveDailyChunkArticle("daily-date-b");
        LocalDate displayDate = LocalDate.of(2026, 10, 20);
        dailyRespository.saveAndFlush(Daily.builder()
                .article(first)
                .startIndex(1)
                .endIndex(2)
                .wordCount(100)
                .displayDate(displayDate)
                .build());

        assertThatThrownBy(() -> dailyRespository.saveAndFlush(Daily.builder()
                .article(second)
                .startIndex(1)
                .endIndex(2)
                .wordCount(100)
                .displayDate(displayDate)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Article saveDailyChunkArticle(String title) {
        return articleRepository.save(Article.builder()
                .title(title + "-" + System.nanoTime())
                .originalText("Daily pipeline integration test.")
                .publishedDate(LocalDate.of(2026, 10, 17))
                .source("TEST")
                .pipelineStage(ArticlePipelineStage.DAILY_CHUNK)
                .pipelineStatus(Status.PENDING)
                .build());
    }

    private void saveSentence(Article article, int orderIndex, Status translationStatus, int wordCount) {
        sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(orderIndex)
                .englishText("word ".repeat(wordCount).trim())
                .koreanText("번역 문장")
                .translationStatus(translationStatus)
                .build());
    }
}
