package com.newslit.backend.crawl;

import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.rss.Rss;
import com.newslit.backend.rss.RssRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CrawlScheduler {
    private final RssRepository rssRepository;
    private final VoaArticleCrawlerService voaArticleCrawlerService;


    @Scheduled(cron = "0 0 0 * * FRI")
    public void scheduledVoaCrawl() {
        Optional<Rss> rss = rssRepository.findFirstByStatusOrderByIdAsc(Status.PENDING);
        rss.ifPresent(value -> voaArticleCrawlerService.crawlAndSaveArticles(value.getId()));
    }
}
