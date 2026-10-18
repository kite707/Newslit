package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsBatchService {
    private static final List<Status> TTS_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final SentenceRepository sentenceRepository;
    private final TtsProperties properties;
    private final TtsStateService stateService;
    private final TtsAsyncService ttsAsyncService;

    public int processBatch() {
        return dispatch(sentenceRepository.findTtsCandidates(
                ArticlePipelineStage.COMPLETED,
                Status.SUCCESS,
                TTS_STATUSES,
                properties.getMaxRetryCount(),
                PageRequest.of(0, properties.getBatchSize())
        ));
    }

    public int processArticle(Long articleId) {
        int resetCount = stateService.resetFinalFailure(articleId);
        if (resetCount > 0) {
            log.info("TTS 최종 실패 상태를 수동 복구 - articleId: {}, 문장: {}건", articleId, resetCount);
        }

        return dispatch(sentenceRepository.findTtsCandidatesByArticleId(
                articleId,
                TTS_STATUSES,
                properties.getMaxRetryCount(),
                PageRequest.of(0, properties.getBatchSize())
        ));
    }

    boolean claim(Long sentenceId) {
        return sentenceRepository.claimTts(
                sentenceId,
                TTS_STATUSES,
                Status.PROCESSING,
                properties.getMaxRetryCount(),
                LocalDateTime.now()
        ) == 1;
    }

    private int dispatch(List<SentenceRepository.TtsCandidate> candidates) {
        int claimedCount = 0;
        for (SentenceRepository.TtsCandidate candidate : candidates) {
            if (!claim(candidate.getSentenceId())) {
                continue;
            }

            claimedCount++;
            ttsAsyncService.generateAsync(candidate.getSentenceId());
        }

        log.info("TTS 배치 선점 완료 - 조회: {}건, 선점: {}건", candidates.size(), claimedCount);
        return claimedCount;
    }
}
