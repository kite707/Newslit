package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SentenceRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    private Article article;

    @BeforeEach
    void setup() {
        article = articleRepository.save(Article.builder()
                .title("stale-processing-" + UUID.randomUUID())
                .originalText("First sentence. Second sentence.")
                .publishedDate(LocalDate.now())
                .source("TEST")
                .build());
    }

    @Test
    void 기준시각보다_오래된_PROCESSING만_FAILED로_복구() {
        Sentence processing = saveSentence(Status.PROCESSING, 1);
        Sentence success = saveSentence(Status.SUCCESS, 2);

        int recoveredCount = sentenceRepository.markStaleProcessingAsFailed(
                LocalDateTime.now().plusMinutes(1), Status.FAILED, Status.PROCESSING);

        assertThat(recoveredCount).isGreaterThanOrEqualTo(1);
        assertThat(findStatus(processing)).isEqualTo(Status.FAILED);
        assertThat(findStatus(success)).isEqualTo(Status.SUCCESS);
    }

    @Test
    void 최근에_PROCESSING이_된_문장은_그대로_둠() {
        Sentence processing = saveSentence(Status.PROCESSING, 1);

        sentenceRepository.markStaleProcessingAsFailed(
                LocalDateTime.now().minusMinutes(10), Status.FAILED, Status.PROCESSING);

        assertThat(findStatus(processing)).isEqualTo(Status.PROCESSING);
    }

    private Sentence saveSentence(Status status, int orderIndex) {
        return sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(orderIndex)
                .englishText("sentence " + orderIndex)
                .koreanText("")
                .translationStatus(status)
                .build());
    }

    private Status findStatus(Sentence sentence) {
        return sentenceRepository.findById(sentence.getId()).orElseThrow().getTranslationStatus();
    }
}
