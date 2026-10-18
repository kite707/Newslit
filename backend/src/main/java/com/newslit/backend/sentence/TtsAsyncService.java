package com.newslit.backend.sentence;

import com.newslit.backend.audio.AudioStorageService;
import com.newslit.backend.audio.TtsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsAsyncService {

    private final TtsStateService ttsStateService;
    private final TtsClient ttsClient;
    private final AudioStorageService audioStorageService;
    private final TtsProperties properties;

    @Async("externalApiExecutor")
    public void generateAsync(Long sentenceId) {
        TtsStateService.TtsWork work = ttsStateService.getWork(sentenceId);
        if (work == null) {
            log.info("선점 상태가 아닌 TTS 작업 스킵 - sentenceId: {}", sentenceId);
            return;
        }

        try {
            byte[] audio = ttsClient.synthesize(work.englishText());
            String audioUrl = audioStorageService.uploadMp3(objectName(work), audio);
            ttsStateService.completeSuccess(sentenceId, audioUrl);
        } catch (Exception e) {
            String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
            TtsStateService.TtsFailure failure = ttsStateService.completeFailure(sentenceId);
            ttsStateService.notifyIfExhausted(
                    failure,
                    properties.getMaxRetryCount(),
                    reason + " sentenceId=" + sentenceId
            );
            logFailure(work, failure, reason, e);
        }
    }

    private String objectName(TtsStateService.TtsWork work) {
        return "sentences/" + work.articleId() + "/" + work.orderIndex() + ".mp3";
    }

    private void logFailure(
            TtsStateService.TtsWork work,
            TtsStateService.TtsFailure failure,
            String reason,
            Exception exception
    ) {
        int retryCount = failure == null ? -1 : failure.retryCount();
        log.error(
                "pipelineFailure=true articleId={} sentenceId={} stage=TTS retryCount={} reason={}",
                work.articleId(),
                work.sentenceId(),
                retryCount,
                reason,
                exception
        );
    }
}
