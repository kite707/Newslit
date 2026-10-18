package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.support.OracleDatabaseTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SentenceRepositoryTest extends OracleDatabaseTestSupport {

    private static final int MAX_RETRY_COUNT = 10;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    private Article article;

    @BeforeEach
    void setup() {
        article = saveArticle(ArticlePipelineStage.TRANSLATION, Status.PROCESSING);
    }

    @Test
    void 기준시각보다_오래된_PROCESSING만_FAILED로_복구() {
        Sentence processing = saveSentence(article, Status.PROCESSING, 1);
        Sentence success = saveSentence(article, Status.SUCCESS, 2);

        int recoveredCount = recoverStale(processing, LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isEqualTo(1);
        assertThat(findStatus(processing)).isEqualTo(Status.FAILED);
        assertThat(reload(processing).getTranslationRetryCount()).isEqualTo(1);
        assertThat(findStatus(success)).isEqualTo(Status.SUCCESS);
    }

    @Test
    void 최근에_PROCESSING이_된_문장은_그대로_둠() {
        Sentence processing = saveSentence(article, Status.PROCESSING, 1);

        int recoveredCount = recoverStale(processing, LocalDateTime.now().minusMinutes(10));

        assertThat(recoveredCount).isZero();
        assertThat(findStatus(processing)).isEqualTo(Status.PROCESSING);
        assertThat(reload(processing).getTranslationRetryCount()).isZero();
    }

    @Test
    void 번역_단계가_아닌_기사의_문장은_복구하지_않는다() {
        Article splitStageArticle = saveArticle(ArticlePipelineStage.SENTENCE_SPLIT, Status.PENDING);
        Sentence processing = saveSentence(splitStageArticle, Status.PROCESSING, 1);

        int recoveredCount = recoverStale(processing, LocalDateTime.now().plusMinutes(1));

        assertThat(recoveredCount).isZero();
        assertThat(findStatus(processing)).isEqualTo(Status.PROCESSING);
        assertThat(reload(processing).getTranslationRetryCount()).isZero();
    }

    /**
     * 프로덕션 호출자(TranslationStateService#recoverStale)와 동일한 파라미터로 호출한다.
     */
    private int recoverStale(Sentence sentence, LocalDateTime threshold) {
        return sentenceRepository.recoverStaleTranslation(
                sentence.getId(),
                ArticlePipelineStage.TRANSLATION,
                List.of(Status.PENDING, Status.PROCESSING),
                threshold,
                Status.FAILED,
                Status.PROCESSING,
                MAX_RETRY_COUNT,
                LocalDateTime.now());
    }

    private Article saveArticle(ArticlePipelineStage stage, Status pipelineStatus) {
        return articleRepository.save(Article.builder()
                .title("stale-processing-" + UUID.randomUUID())
                .originalText("First sentence. Second sentence.")
                .publishedDate(LocalDate.now())
                .source("TEST")
                .pipelineStage(stage)
                .pipelineStatus(pipelineStatus)
                .build());
    }

    private Sentence saveSentence(Article owner, Status status, int orderIndex) {
        return sentenceRepository.save(Sentence.builder()
                .article(owner)
                .orderIndex(orderIndex)
                .englishText("sentence " + orderIndex)
                .koreanText("")
                .translationStatus(status)
                .build());
    }

    private Sentence reload(Sentence sentence) {
        return sentenceRepository.findById(sentence.getId()).orElseThrow();
    }

    private Status findStatus(Sentence sentence) {
        return reload(sentence).getTranslationStatus();
    }
}
