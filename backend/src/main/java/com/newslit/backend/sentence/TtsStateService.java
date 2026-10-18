package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TtsStateService {

    private final SentenceRepository sentenceRepository;

    @Transactional(readOnly = true)
    public TtsWork getWork(Long sentenceId) {
        return sentenceRepository.findTtsWork(sentenceId, Status.PROCESSING)
                .map(sentence -> new TtsWork(
                        sentence.getId(),
                        sentence.getArticle().getId(),
                        sentence.getOrderIndex(),
                        sentence.getEnglishText()))
                .orElse(null);
    }

    @Transactional
    public void completeSuccess(Long sentenceId, String audioUrl) {
        sentenceRepository.findById(sentenceId).ifPresent(sentence -> {
            if (sentence.getTtsStatus() != Status.PROCESSING) {
                return;
            }
            sentence.setAudioUrl(audioUrl);
            sentence.setTtsStatus(Status.SUCCESS);
        });
    }

    @Transactional
    public TtsFailure completeFailure(Long sentenceId) {
        Sentence sentence = sentenceRepository.findById(sentenceId).orElse(null);
        if (sentence == null || sentence.getTtsStatus() != Status.PROCESSING) {
            return null;
        }

        sentence.incrementTtsRetryCount();
        sentence.setTtsStatus(Status.FAILED);
        return new TtsFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTtsRetryCount()
        );
    }

    @Transactional
    public TtsFailure recoverStale(Long sentenceId, LocalDateTime threshold, int maxRetryCount) {
        int recovered = sentenceRepository.recoverStaleTts(
                sentenceId,
                threshold,
                Status.FAILED,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        );
        if (recovered == 0) {
            return null;
        }

        Sentence sentence = sentenceRepository.findById(sentenceId).orElseThrow();
        return new TtsFailure(
                sentence.getId(),
                sentence.getArticle().getId(),
                sentence.getTtsRetryCount()
        );
    }

    /**
     * 재시도 한도를 모두 소진해 FAILED로 멈춘 문장의 TTS 재시도 횟수를 0으로 되돌린다.
     * 운영자가 수동 API로 호출하는 복구 경로이며, 진행 중(PROCESSING)인 문장은 건드리지 않고
     * stale 복구에 맡긴다.
     */
    @Transactional
    public int resetFinalFailure(Long articleId) {
        return sentenceRepository.resetFailedTtsRetryCounts(
                articleId,
                Status.FAILED,
                LocalDateTime.now()
        );
    }

    public record TtsWork(Long sentenceId, Long articleId, Integer orderIndex, String englishText) {
    }

    public record TtsFailure(Long sentenceId, Long articleId, int retryCount) {
    }
}
