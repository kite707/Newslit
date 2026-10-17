package com.newslit.backend.sentence;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticlePipelineStage;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SentenceService {
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;
    private final TranslationBatchService translationBatchService;

    @Transactional
    public List<Sentence> splitSentences(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        List<Sentence> existingSentences = sentenceRepository.findAllByArticleIdOrderByOrderIndexAsc(articleId);
        if (article.getPipelineStage() != ArticlePipelineStage.SENTENCE_SPLIT) {
            return existingSentences;
        }

        List<String> sentenceTexts = splitIntoSentencesAdvanced(article.getOriginalText());
        Map<Integer, Sentence> sentencesByOrder = new HashMap<>();
        existingSentences.forEach(sentence -> sentencesByOrder.put(sentence.getOrderIndex(), sentence));

        for (int i = 0; i < sentenceTexts.size(); i++) {
            int orderIndex = i + 1;
            if (sentencesByOrder.containsKey(orderIndex)) {
                continue;
            }

            Sentence sentence = createNewSentence(article, sentenceTexts.get(i), orderIndex);
            sentencesByOrder.put(orderIndex, sentence);
        }

        if (!sentenceTexts.isEmpty()) {
            article.advancePipelineTo(ArticlePipelineStage.TRANSLATION);
        }

        return sentenceRepository.findAllByArticleIdOrderByOrderIndexAsc(articleId);
    }

    public void triggerTranslation(Long articleId) {
        translationBatchService.processArticle(articleId);
    }

    public List<SentenceResponseDto> getSentences(Long articleId) {
        return sentenceRepository.findAllByArticleIdOrderByOrderIndexAsc(articleId).stream()
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
