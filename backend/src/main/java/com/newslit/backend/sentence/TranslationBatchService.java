package com.newslit.backend.sentence;

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
public class TranslationBatchService {
    private static final List<Status> PIPELINE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);
    private static final List<Status> TRANSLATION_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final ArticleRepository articleRepository;
    private final SentenceRepository sentenceRepository;
    private final TranslationProperties properties;
    private final TranslationClaimService claimService;
    private final TranslationStateService stateService;
    private final TranslationAsyncService translationAsyncService;

    public int processBatch() {
        reconcileCompletedArticles();
        return dispatch(sentenceRepository.findTranslationCandidates(
                ArticlePipelineStage.TRANSLATION,
                PIPELINE_STATUSES,
                TRANSLATION_STATUSES,
                properties.getMaxRetryCount(),
                PageRequest.of(0, properties.getBatchSize())
        ));
    }

    public int processArticle(Long articleId) {
        stateService.advanceArticleIfComplete(articleId);
        List<SentenceRepository.TranslationCandidate> candidates = sentenceRepository
                .findTranslationCandidatesByArticleId(
                        articleId,
                        ArticlePipelineStage.TRANSLATION,
                        PIPELINE_STATUSES,
                        TRANSLATION_STATUSES,
                        properties.getMaxRetryCount(),
                        PageRequest.of(0, properties.getBatchSize()));
        return dispatch(candidates);
    }

    private void reconcileCompletedArticles() {
        List<Long> completedArticleIds = articleRepository.findTranslationCompletedArticleIds(
                ArticlePipelineStage.TRANSLATION,
                PIPELINE_STATUSES,
                Status.SUCCESS,
                PageRequest.of(0, properties.getBatchSize())
        );

        int reconciledCount = 0;
        for (Long articleId : completedArticleIds) {
            if (stateService.advanceArticleIfComplete(articleId)) {
                reconciledCount++;
            }
        }

        if (reconciledCount > 0) {
            log.info("번역 완료 상태 재조정 - 대상: {}건, 전이: {}건", completedArticleIds.size(), reconciledCount);
        }
    }

    private int dispatch(List<SentenceRepository.TranslationCandidate> candidates) {
        int claimedCount = 0;
        for (SentenceRepository.TranslationCandidate candidate : candidates) {
            if (!claimService.claim(
                    candidate.getSentenceId(),
                    candidate.getArticleId(),
                    properties.getMaxRetryCount())) {
                continue;
            }

            claimedCount++;
            translationAsyncService.translateAsync(candidate.getSentenceId());
        }

        log.info("번역 배치 선점 완료 - 조회: {}건, 선점: {}건", candidates.size(), claimedCount);
        return claimedCount;
    }
}
