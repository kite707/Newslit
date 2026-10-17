package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TranslationClaimService {
    private static final List<Status> PIPELINE_STATUSES = List.of(Status.PENDING, Status.PROCESSING);
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public boolean claim(Long sentenceId, Long articleId, int maxRetryCount) {
        LocalDateTime claimedAt = LocalDateTime.now();
        int claimed = sentenceRepository.claimTranslation(
                sentenceId,
                ArticlePipelineStage.TRANSLATION,
                PIPELINE_STATUSES,
                CLAIMABLE_STATUSES,
                Status.PROCESSING,
                maxRetryCount,
                claimedAt
        );

        if (claimed == 0) {
            return false;
        }

        articleRepository.markTranslationProcessing(
                articleId,
                ArticlePipelineStage.TRANSLATION,
                Status.PENDING,
                Status.PROCESSING,
                claimedAt
        );
        return true;
    }
}
