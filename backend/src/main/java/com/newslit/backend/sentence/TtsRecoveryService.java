package com.newslit.backend.sentence;

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
public class TtsRecoveryService {

    private final SentenceRepository sentenceRepository;
    private final TtsProperties properties;
    private final TtsStateService stateService;

    public int recoverStaleProcessing() {
        return recoverStaleProcessing(
                LocalDateTime.now().minusMinutes(properties.getStaleProcessingMinutes())
        );
    }

    int recoverStaleProcessing(LocalDateTime threshold) {
        List<SentenceRepository.TtsCandidate> candidates = sentenceRepository.findStaleTtsCandidates(
                Status.PROCESSING,
                properties.getMaxRetryCount(),
                threshold,
                PageRequest.of(0, properties.getBatchSize())
        );

        int recoveredCount = 0;
        for (SentenceRepository.TtsCandidate candidate : candidates) {
            TtsStateService.TtsFailure failure = stateService.recoverStale(
                    candidate.getSentenceId(),
                    threshold,
                    properties.getMaxRetryCount()
            );
            if (failure == null) {
                continue;
            }

            recoveredCount++;
            log.warn("pipelineRecovery=true articleId={} sentenceId={} stage=TTS retryCount={} reason=stale-processing",
                    failure.articleId(),
                    failure.sentenceId(),
                    failure.retryCount());
            stateService.notifyIfExhausted(
                    failure,
                    properties.getMaxRetryCount(),
                    "stale-processing sentenceId=" + failure.sentenceId()
            );
        }
        return recoveredCount;
    }
}
