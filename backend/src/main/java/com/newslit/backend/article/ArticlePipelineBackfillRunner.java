package com.newslit.backend.article;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePipelineBackfillRunner implements ApplicationRunner {

    private final ArticlePipelineBackfillService backfillService;

    @Override
    public void run(ApplicationArguments args) {
        ArticlePipelineBackfillService.BackfillResult result = backfillService.backfillLegacyArticles();
        if (result.total() == 0) {
            return;
        }

        log.info(
                "기존 Article 파이프라인 상태 보정 완료 - translation: {}건, dailyChunk: {}건, completed: {}건",
                result.translation(),
                result.dailyChunk(),
                result.completed()
        );
    }
}
