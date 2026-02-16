package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import com.newslit.backend.sentence.exception.TranslationFailedException;
import com.newslit.backend.sentence.exception.TranslationInterruptedException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentenceService {
    private final DeepLClient deepLClient;
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    private static final int MAX_RETRY_CNT = 3;

    public List<SentenceResponseDto> translateOneParagraph(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        String paragraph = article.getOriginalText();
        List<String> sentences = splitIntoSentencesAdvanced(paragraph);
        List<SentenceResponseDto> responses = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            SentenceResponseDto response = translateSingleSentence(articleId, article, sentences.get(i), i + 1);
            responses.add(response);
        }

        return responses;
    }

    public SentenceResponseDto translateSingleSentence(Long articleId, Article article, String englishText,
                                                       int orderIndex) {

        Optional<Sentence> existing = sentenceRepository
                .findByArticleIdAndEnglishText(articleId, englishText);

        //이미 번역 성공하여 저장되어있으면 pass
        if (existing.isPresent() && existing.get().getTranslationStatus() == Status.SUCCESS) {
            return toSentenceDto(existing.get());
        }

        //실패 상태거나, 없는 문장이면 해석 API 호출
        Sentence sentence = existing.orElseGet(() -> createNewSentence(article, englishText, orderIndex));

        return processTranslation(sentence, englishText, articleId, orderIndex);
    }

    private Sentence createNewSentence(Article article, String englishText, int orderIndex) {
        return sentenceRepository.save(
                Sentence.builder()
                        .article(article)
                        .translationStatus(Status.PENDING)
                        .orderIndex(orderIndex)
                        .englishText(englishText)
                        .koreanText("")
                        .build()
        );
    }

    public void retryTranslation(Sentence sentence) throws DeepLException, InterruptedException {
        sentence.incrementRetryCount();
        try {
            String translatedText = deepLClient
                    .translateText(sentence.getEnglishText(), null, "ko")
                    .getText();

            sentence.setKoreanText(translatedText);
            sentence.setTranslationStatus(Status.SUCCESS);

        } catch (Exception e) {
            log.warn("스케줄링 재처리 실패 - Sentence ID: {}, Error: {}",
                    sentence.getId(), e.getMessage());
            throw e;
        } finally {
            sentenceRepository.save(sentence);
        }
    }

    private SentenceResponseDto processTranslation(Sentence sentence, String englishText,
                                                   Long articleId, int orderIndex) {
        sentence.setTranslationStatus(Status.PROCESSING);
        sentenceRepository.save(sentence);

        for (int retryCnt = 0; retryCnt < MAX_RETRY_CNT; retryCnt++) {
            try {
                if (retryCnt > 0) {
                    Thread.sleep(500);
                }
                String translatedText = deepLClient.translateText(englishText, null, "ko").getText();

                sentence.setKoreanText(translatedText);
                sentence.setTranslationStatus(Status.SUCCESS);
                sentenceRepository.save(sentence);

                return SentenceResponseDto.builder()
                        .articleId(articleId)
                        .orderIndex(orderIndex)
                        .englishText(englishText)
                        .koreanText(translatedText)
                        .status(Status.SUCCESS)
                        .build();
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    sentence.setTranslationStatus(Status.FAILED);
                    sentenceRepository.save(sentence);
                    throw new TranslationInterruptedException();
                }
            }
        }
        sentence.setTranslationStatus(Status.FAILED);
        sentenceRepository.save(sentence);
        throw new TranslationFailedException();
    }

    private List<String> splitIntoSentencesAdvanced(String text) {
        List<String> sentences = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        // 구분선 제거
        text = text.replaceAll("(?m)^[\\-_=]{4,}\\s*$", "");

        BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        boundary.setText(text);

        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty() && sentence.length() > 1) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    private SentenceResponseDto toSentenceDto(Sentence sentence) {
        return SentenceResponseDto.builder()
                .articleId(sentence.getArticle().getId())
                .orderIndex(sentence.getOrderIndex())
                .englishText(sentence.getEnglishText())
                .koreanText(sentence.getKoreanText())
                .status(sentence.getTranslationStatus())
                .build();
    }

}
