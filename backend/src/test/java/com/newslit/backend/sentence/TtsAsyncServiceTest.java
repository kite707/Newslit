package com.newslit.backend.sentence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newslit.backend.audio.AudioStorageService;
import com.newslit.backend.audio.TtsClient;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TtsAsyncServiceTest {

    private static final Long SENTENCE_ID = 7L;
    private static final Long ARTICLE_ID = 1L;
    private static final int MAX_RETRY_COUNT = 3;

    @Mock
    private TtsStateService ttsStateService;
    @Mock
    private TtsClient ttsClient;
    @Mock
    private AudioStorageService audioStorageService;

    private final TtsProperties properties = new TtsProperties();
    private TtsAsyncService ttsAsyncService;

    @BeforeEach
    void setUp() {
        properties.setMaxRetryCount(MAX_RETRY_COUNT);
        ttsAsyncService = new TtsAsyncService(
                ttsStateService,
                ttsClient,
                audioStorageService,
                properties
        );
    }

    @Test
    void 성공하면_문장_번호로_업로드하고_오디오_URL을_저장한다() throws Exception {
        byte[] audio = {1, 2};
        when(ttsStateService.getWork(SENTENCE_ID)).thenReturn(work());
        when(ttsClient.synthesize("Sentence 3.")).thenReturn(audio);
        when(audioStorageService.uploadMp3("sentences/1/3.mp3", audio)).thenReturn("https://storage/3.mp3");

        ttsAsyncService.generateAsync(SENTENCE_ID);

        verify(ttsStateService).completeSuccess(SENTENCE_ID, "https://storage/3.mp3");
    }

    @Test
    void 선점_상태가_아닌_문장은_TTS를_호출하지_않는다() throws Exception {
        when(ttsStateService.getWork(SENTENCE_ID)).thenReturn(null);

        ttsAsyncService.generateAsync(SENTENCE_ID);

        verify(ttsClient, never()).synthesize(anyString());
        verify(audioStorageService, never()).uploadMp3(anyString(), any());
        verify(ttsStateService, never()).completeFailure(anyLong());
    }

    @Test
    void TTS_호출이_실패하면_실패_처리하고_한도_도달_여부를_알린다() throws Exception {
        TtsStateService.TtsFailure failure =
                new TtsStateService.TtsFailure(SENTENCE_ID, ARTICLE_ID, MAX_RETRY_COUNT);
        when(ttsStateService.getWork(SENTENCE_ID)).thenReturn(work());
        when(ttsClient.synthesize("Sentence 3.")).thenThrow(new IOException("TTS API 실패: 500"));
        when(ttsStateService.completeFailure(SENTENCE_ID)).thenReturn(failure);

        ttsAsyncService.generateAsync(SENTENCE_ID);

        verify(ttsStateService, never()).completeSuccess(anyLong(), anyString());
        verify(ttsStateService).notifyIfExhausted(
                eq(failure),
                eq(MAX_RETRY_COUNT),
                eq("IOException: TTS API 실패: 500 sentenceId=" + SENTENCE_ID)
        );
    }

    @Test
    void 업로드가_실패해도_실패_처리한다() throws Exception {
        when(ttsStateService.getWork(SENTENCE_ID)).thenReturn(work());
        when(ttsClient.synthesize("Sentence 3.")).thenReturn(new byte[]{1});
        when(audioStorageService.uploadMp3(anyString(), any()))
                .thenThrow(new IllegalStateException("업로드 실패"));

        ttsAsyncService.generateAsync(SENTENCE_ID);

        verify(ttsStateService).completeFailure(SENTENCE_ID);
        verify(ttsStateService, never()).completeSuccess(anyLong(), anyString());
    }

    private TtsStateService.TtsWork work() {
        return new TtsStateService.TtsWork(SENTENCE_ID, ARTICLE_ID, 3, "Sentence 3.");
    }
}
