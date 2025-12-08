package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SentenceService {
    private final DeepLClient deepLClient;
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    public List<SentenceResponseDto> translateOneParagraph(Long articleId) throws DeepLException, InterruptedException {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleNotFoundException());

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
                                                       int orderIndex)
            throws DeepLException, InterruptedException {
        Sentence sentence = Sentence.builder()
                .article(article)
                .translationStatus(Status.PROCESSING)
                .orderIndex(orderIndex)
                .englishText(englishText)
                .koreanText(" ")
                .build();
        sentenceRepository.save(sentence);

        String translatedText = deepLClient.translateText(englishText, null, "ko").getText();
        sentence.setKoreanText(translatedText);
        sentence.setTranslationStatus(Status.SUCCESS);

        return SentenceResponseDto.builder()
                .articleId(articleId)
                .orderIndex(orderIndex)
                .englishText(englishText)
                .koreanText(translatedText)
                .status(sentence.getTranslationStatus())
                .build();
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

}
