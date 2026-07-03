package com.newslit.backend.global.setup;

import com.newslit.backend.crawl.VoaRssCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("prod")
public class RssCrawlInitializer implements ApplicationRunner {
    private final VoaRssCrawlerService voaRssCrawlerService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("RSS 초기화 시작");
        voaRssCrawlerService.crawlVoaRssLinks();
        log.info("RSS 초기화 완료");
    }
}
