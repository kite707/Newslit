package com.newslit.backend.sentence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newslit.backend.audio.AudioStorageService;
import com.newslit.backend.audio.TtsClient;
import com.newslit.backend.global.common.enums.Status;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SentenceTtsServiceTest {

    private static final Long ARTICLE_ID = 1L;

    @Mock
    private SentenceRepository sentenceRepository;
    @Mock
    private TtsClient ttsClient;
    @Mock
    private AudioStorageService audioStorageService;
    @InjectMocks
    private SentenceTtsService sentenceTtsService;

    @Test
    void 성공하면_오디오_URL을_저장하고_SUCCESS로_바꾼다() throws Exception {
        Sentence sentence = sentence(3, Status.PENDING);
        byte[] audio = {1, 2};
        when(sentenceRepository.findAllByArticleId(ARTICLE_ID)).thenReturn(List.of(sentence));
        when(ttsClient.synthesize("Sentence 3.")).thenReturn(audio);
        when(audioStorageService.uploadMp3("sentences/1/3.mp3", audio)).thenReturn("https://storage/3.mp3");

        sentenceTtsService.generateAudio(ARTICLE_ID);

        assertThat(sentence.getTtsStatus()).isEqualTo(Status.SUCCESS);
        assertThat(sentence.getAudioUrl()).isEqualTo("https://storage/3.mp3");
    }

    @Test
    void 이미_SUCCESS인_문장은_건너뛴다() throws Exception {
        Sentence done = sentence(1, Status.SUCCESS);
        when(sentenceRepository.findAllByArticleId(ARTICLE_ID)).thenReturn(List.of(done));

        sentenceTtsService.generateAudio(ARTICLE_ID);

        verify(ttsClient, never()).synthesize(anyString());
        verify(audioStorageService, never()).uploadMp3(anyString(), any());
    }

    @Test
    void 한_문장이_실패해도_FAILED로_남기고_다음_문장을_계속_처리한다() throws Exception {
        Sentence failing = sentence(1, Status.PENDING);
        Sentence next = sentence(2, Status.PENDING);
        byte[] audio = {1};
        when(sentenceRepository.findAllByArticleId(ARTICLE_ID)).thenReturn(List.of(failing, next));
        when(ttsClient.synthesize("Sentence 1.")).thenThrow(new IOException("TTS API 실패: 500"));
        when(ttsClient.synthesize("Sentence 2.")).thenReturn(audio);
        when(audioStorageService.uploadMp3(eq("sentences/1/2.mp3"), any())).thenReturn("https://storage/2.mp3");

        sentenceTtsService.generateAudio(ARTICLE_ID);

        assertThat(failing.getTtsStatus()).isEqualTo(Status.FAILED);
        assertThat(failing.getAudioUrl()).isNull();
        assertThat(next.getTtsStatus()).isEqualTo(Status.SUCCESS);
    }

    @Test
    void FAILED였던_문장은_다시_시도한다() throws Exception {
        Sentence failed = sentence(1, Status.FAILED);
        when(sentenceRepository.findAllByArticleId(ARTICLE_ID)).thenReturn(List.of(failed));
        when(ttsClient.synthesize("Sentence 1.")).thenReturn(new byte[]{1});
        when(audioStorageService.uploadMp3(anyString(), any())).thenReturn("https://storage/1.mp3");

        sentenceTtsService.generateAudio(ARTICLE_ID);

        assertThat(failed.getTtsStatus()).isEqualTo(Status.SUCCESS);
    }

    private Sentence sentence(int orderIndex, Status ttsStatus) {
        return Sentence.builder()
                .orderIndex(orderIndex)
                .englishText("Sentence " + orderIndex + ".")
                .ttsStatus(ttsStatus)
                .build();
    }
}
