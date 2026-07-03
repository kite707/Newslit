package com.newslit.backend.crawl;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final VoaArticleCrawlerService voaArticleCrawlerService;
    private final VoaRssCrawlerService voaRssCrawlerService;


    @PostMapping("/voa/run")
    public ResponseEntity<String> runVoaCrawler(@RequestParam Long rssId) {
        voaArticleCrawlerService.crawlAndSaveArticles(rssId);
        return ResponseEntity.ok("Voa 크롤링 완료");
    }

    @PostMapping("/rss/run")
    public ResponseEntity<String> runRssCrawler() throws IOException {
        voaRssCrawlerService.crawlVoaRssLinks();
        return ResponseEntity.ok("RSS 크롤링 완료");
    }
}
