package com.newslit.backend.daily;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyPipelineService {
    private final ArticleRepository articleRepository;
    private final DailyPipelineProperties properties;
    private final DailyPipelineStateService stateService;
    private final DailyService dailyService;

    public int processBatch() {
        List<Long> articleIds = articleRepository.findPipelineCandidateIds(
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PENDING,
                PageRequest.of(0, properties.getBatchSize())
        );

        int claimedCount = 0;
        for (Long articleId : articleIds) {
            if (!stateService.claim(articleId)) {
                continue;
            }

            claimedCount++;
            processClaimedArticle(articleId);
        }

        log.info("Daily 배치 처리 완료 - 조회: {}건, 선점: {}건", articleIds.size(), claimedCount);
        return claimedCount;
    }

    private void processClaimedArticle(Long articleId) {
        try {
            int chunkCount = dailyService.createChunks(articleId).size();
            if (chunkCount == 0) {
                throw new IllegalStateException("생성된 Daily 청크가 없습니다");
            }
            if (!stateService.complete(articleId)) {
                throw new IllegalStateException("Daily 완료 상태 전이에 실패했습니다");
            }
            log.info("Daily 생성 성공 - articleId: {}, chunkCount: {}", articleId, chunkCount);
        } catch (Exception e) {
            stateService.fail(articleId);
            log.error("Daily 생성 실패 - articleId: {}, error: {}", articleId, e.getMessage(), e);
        }
    }
}
