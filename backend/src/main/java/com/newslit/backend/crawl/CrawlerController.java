package com.newslit.backend.crawl;

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

    private final VoaCrawlerService crawlerService;

    @PostMapping("/voa/run")
    public ResponseEntity<String> runCrawler(@RequestParam Long rssId) {
        crawlerService.crawlAndSaveArticles(rssId);
        return ResponseEntity.ok("크롤링 완료");
    }
}
