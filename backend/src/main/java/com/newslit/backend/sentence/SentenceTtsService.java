package com.newslit.backend.sentence;

import com.newslit.backend.audio.AudioStorageService;
import com.newslit.backend.audio.TtsClient;
import com.newslit.backend.global.common.enums.Status;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentenceTtsService {

    private final SentenceRepository sentenceRepository;
    private final TtsClient ttsClient;
    private final AudioStorageService audioStorageService;

    @Async("externalApiExecutor")
    public void generateAudio(Long articleId) {
        List<Sentence> sentences = sentenceRepository.findAllByArticleId(articleId);

        for (Sentence sentence : sentences) {
            if (sentence.getTtsStatus() == Status.SUCCESS) {
                continue;
            }
            generateSentenceAudio(sentence, articleId);
        }
    }

    private void generateSentenceAudio(Sentence sentence, Long articleId) {
        sentence.setTtsStatus(Status.PROCESSING);
        sentenceRepository.save(sentence);

        try {
            byte[] audio = ttsClient.synthesize(sentence.getEnglishText());
            String audioUrl = audioStorageService.uploadMp3(objectName(articleId, sentence), audio);

            sentence.setAudioUrl(audioUrl);
            sentence.setTtsStatus(Status.SUCCESS);
        } catch (Exception e) {
            log.error("TTS 생성 실패 - sentenceId: {}, articleId: {}", sentence.getId(), articleId, e);
            sentence.setTtsStatus(Status.FAILED);
        } finally {
            sentenceRepository.save(sentence);
        }
    }

    private String objectName(Long articleId, Sentence sentence) {
        return "sentences/" + articleId + "/" + sentence.getOrderIndex() + ".mp3";
    }
}
