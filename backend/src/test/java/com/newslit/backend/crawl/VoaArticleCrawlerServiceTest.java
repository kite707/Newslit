package com.newslit.backend.crawl;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.daily.DailyRespository;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.vocabulary.VocabularyRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoaArticleCrawlerServiceTest {

    @Autowired
    private VoaArticleCrawlerService voaArticleCrawlerService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private DailyRespository dailyRespository;

    @BeforeAll
    void setup() {
        dailyRespository.deleteAll();
        sentenceRepository.deleteAll();
        vocabularyRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void 동시성_테스트() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    voaArticleCrawlerService.crawlAndSaveArticles(1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(articleRepository.count()).isEqualTo(9L);
    }
}