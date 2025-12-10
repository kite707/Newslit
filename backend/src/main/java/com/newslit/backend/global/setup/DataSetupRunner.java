package com.newslit.backend.global.setup;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.crawl.VoaArticleCrawlerService;
import com.newslit.backend.crawl.VoaRssCrawlerService;
import com.newslit.backend.daily.DailyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DataSetupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSetupRunner.class);


    private final VoaRssCrawlerService voaRssCrawlerService;
    private final VoaArticleCrawlerService voaArticleCrawlerService;
    private final DailyService dailyService;
    private final ArticleRepository articleRepository;

    @Override
    public void run(String... args) throws Exception {
        voaRssCrawlerService.crawlVoaRssLinks();
        log.debug("RSS 크롤링 완료");
        voaArticleCrawlerService.crawlAndSaveArticles(1L);
        log.debug("RSS 1 기사 크롤링 완료");

        List<Article> articles = articleRepository.findAll();
        articles.forEach(article -> {
            dailyService.createChunks(article.getId());
            log.debug("articleId : {} split 완료", article.getId());
        });
    }
}
