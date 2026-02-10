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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SentenceService {
    private final DeepLClient deepLClient;
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

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

    @Transactional
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

    private SentenceResponseDto processTranslation(Sentence sentence, String englishText,
                                                   Long articleId, int orderIndex) {

        sentence.setTranslationStatus(Status.PROCESSING);
        sentenceRepository.save(sentence);

        try {
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
            sentence.setTranslationStatus(Status.FAILED);
            sentenceRepository.save(sentence);

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new TranslationInterruptedException();
            }
            if (e instanceof DeepLException) {
                throw new TranslationFailedException();
            }
            throw new TranslationFailedException();
        }

    }

    private List<String> splitIntoSentencesAdvanced(String text) {
        List<String> sentences = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        // 구분선 제거
        text = text.replaceAll("(?m)^[\\-_=]{4,}\\s*$", "");

        java.text.BreakIterator boundary = java.text.BreakIterator.getSentenceInstance(java.util.Locale.ENGLISH);
        boundary.setText(text);

        int start = boundary.first();
        for (int end = boundary.next(); end != java.text.BreakIterator.DONE; start = end, end = boundary.next()) {
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
                .status(sentence.getTranslationStatus())
                .build();
    }

}
