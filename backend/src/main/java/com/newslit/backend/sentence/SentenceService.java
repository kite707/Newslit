package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.io.IOException;
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
    private final TranslationAsyncService translationAsyncService;


    public void triggerTranslation(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        String paragraph = article.getOriginalText();
        List<String> sentenceTexts = splitIntoSentencesAdvanced(paragraph);

        for (int i = 0; i < sentenceTexts.size(); i++) {
            String englishText = sentenceTexts.get(i);
            int orderIndex = i + 1;

            Optional<Sentence> existing = sentenceRepository.findByArticleIdAndEnglishText(articleId, englishText);

            if (existing.isPresent() && existing.get().getTranslationStatus() == Status.SUCCESS) {
                continue;
            } else if (existing.isPresent() && existing.get().getTranslationStatus() == Status.PROCESSING) {
                log.info("번역 진행 중인 문장 스킵 - orderIndex: {}", orderIndex);
            } else {
                Sentence sentence = existing.orElseGet(() -> createNewSentence(article, englishText, orderIndex));
                translationAsyncService.translateAsync(sentence, englishText, articleId, orderIndex);
            }
        }
    }

    public List<SentenceResponseDto> getSentences(Long articleId) {
        return sentenceRepository.findAllByArticleId(articleId).stream()
                .map(this::toSentenceDto)
                .toList();
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

    public void retryTranslation(Sentence sentence) throws DeepLException, InterruptedException, IOException {
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
