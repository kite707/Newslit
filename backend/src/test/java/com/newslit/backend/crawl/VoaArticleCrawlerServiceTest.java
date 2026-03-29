package com.newslit.backend.crawl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.daily.DailyRespository;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.rss.Rss;
import com.newslit.backend.rss.RssRepository;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.vocabulary.VocabularyRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VoaArticleCrawlerServiceTest {

    private static final String RSS_PATH = "/api/zjypq_l-vomx-tpebryqy";
    private static final int ARTICLE_COUNT = 3;

    @MockBean
    private RssFeedReader rssFeedReader;

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

    @Autowired
    private RssRepository rssRepository;

    private MockWebServer mockWebServer;
    private Long rssId;

    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                return new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "text/html; charset=UTF-8")
                        .setBody(buildArticleHtml(request.getPath()));
            }
        });
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort();

        SyndFeedImpl feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        List<SyndEntry> entries = new ArrayList<>();
        for (int i = 1; i <= ARTICLE_COUNT; i++) {
            SyndEntryImpl entry = new SyndEntryImpl();
            entry.setTitle("Test Article " + i);
            entry.setLink(baseUrl + "/a/article-" + i);
            entries.add(entry);
        }
        feed.setEntries(entries);
        when(rssFeedReader.read(anyString())).thenReturn(feed);

        dailyRespository.deleteAll();
        sentenceRepository.deleteAll();
        vocabularyRepository.deleteAll();
        articleRepository.deleteAll();
        rssRepository.deleteAll();

        Rss rss = rssRepository.save(Rss.builder()
                .category("Learning English")
                .title("VOA Learning English")
                .url(RSS_PATH)
                .status(Status.PENDING)
                .build());
        rssId = rss.getId();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void 동시성_테스트() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    voaArticleCrawlerService.crawlAndSaveArticles(rssId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(articleRepository.count()).isEqualTo(ARTICLE_COUNT);
    }

    private String buildArticleHtml(String path) {
        String title = path.replace("/a/", "").replace("-", " ");
        return "<html><head><meta name=\"pubdate\" content=\"2024-01-01\"></head><body>" +
                "<h1 class=\"title pg-title\">" + title + "</h1>" +
                "<div class=\"wsw\"><p>Test article content for concurrency testing.</p></div>" +
                "</body></html>";
    }
}
