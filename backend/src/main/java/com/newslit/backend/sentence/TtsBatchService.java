package com.newslit.backend.sentence;

import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.global.common.enums.Status;
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
    private final TtsClaimService claimService;
    private final TtsStateService stateService;
    private final TtsAsyncService ttsAsyncService;

    /**
     * 파이프라인이 끝난(COMPLETED/SUCCESS) Article의 문장만 자동 대상으로 삼는다.
     * Daily가 만들어지지 않은 Article에 TTS 비용을 쓰지 않기 위한 조건이다.
     */
    public int processBatch() {
        return dispatch(sentenceRepository.findTtsCandidates(
                ArticlePipelineStage.COMPLETED,
                Status.SUCCESS,
                TTS_STATUSES,
                properties.getMaxRetryCount(),
                PageRequest.of(0, properties.getBatchSize())
        ));
    }

    /**
     * 운영 복구용 수동 경로. 재시도 한도를 소진한 문장을 되돌린 뒤 선점 프로토콜을 그대로 거친다.
     * 자동 배치와 같은 틱당 상한을 적용하므로, 문장이 상한보다 많으면 반복 호출하거나
     * 자동 스케줄러가 남은 문장을 처리한다.
     */
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

    private int dispatch(List<SentenceRepository.TtsCandidate> candidates) {
        int claimedCount = 0;
        for (SentenceRepository.TtsCandidate candidate : candidates) {
            if (!claimService.claim(candidate.getSentenceId(), properties.getMaxRetryCount())) {
                continue;
            }

            claimedCount++;
            ttsAsyncService.generateAsync(candidate.getSentenceId());
        }

        log.info("TTS 배치 선점 완료 - 조회: {}건, 선점: {}건", candidates.size(), claimedCount);
        return claimedCount;
    }
}
