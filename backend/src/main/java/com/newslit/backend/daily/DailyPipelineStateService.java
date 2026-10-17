package com.newslit.backend.daily;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyPipelineStateService {
    private final ArticleRepository articleRepository;

    @Transactional
    public boolean claim(Long articleId) {
        return articleRepository.claimDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PENDING,
                Status.PROCESSING,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public boolean complete(Long articleId) {
        return articleRepository.completeDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                ArticlePipelineStage.COMPLETED,
                Status.PROCESSING,
                Status.SUCCESS,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public boolean fail(Long articleId) {
        return articleRepository.failDailyChunk(
                articleId,
                ArticlePipelineStage.DAILY_CHUNK,
                Status.PROCESSING,
                Status.FAILED,
                LocalDateTime.now()
        ) == 1;
    }
}
