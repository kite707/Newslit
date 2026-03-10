package com.newslit.backend.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.ArticleService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VoaArticleCrawlerServiceTest {

    @Autowired
    public VoaArticleCrawlerService voaArticleCrawlerService;

    @Autowired
    public ArticleRepository articleRepository;

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

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        assertThat(articleRepository.count()).isEqualTo(9L);

    }


}