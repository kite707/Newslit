package com.newslit.backend.audio;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.audio.dto.WordDto;
import com.newslit.backend.sentence.Sentence;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AudioPersistenceService {

    private final ArticleRepository articleRepository;

    @Transactional
    public void persistResults(Long articleId, String audioUrl, List<WordDto> words) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        article.setAudioLink(audioUrl);
        List<Sentence> sentences = article.getSentences().stream()
                .sorted(Comparator.comparing(Sentence::getOrderIndex))
                .collect(Collectors.toList());

        matchAndSaveTimestamps(words, sentences);
    }


    private void matchAndSaveTimestamps(List<WordDto> words, List<Sentence> sentences) {
        if (words.isEmpty()) {
            return;
        }
        int wordsStart = 0;
        for (Sentence sentence : sentences) {

            int endIdx = getEndIdx(sentence, words, wordsStart);

            if (endIdx < wordsStart || endIdx >= words.size()) {
                continue;
            }

            sentence.setStartTime(words.get(wordsStart).getStart());
            sentence.setEndTime(words.get(endIdx).getEnd());

            wordsStart = endIdx + 1;
        }
    }

    private int getEndIdx(Sentence sentence, List<WordDto> words, int start) {
        int startIdx = start;
        List<String> sentenceWords = List.of(sentence.getEnglishText().split(" "));
        for (String word : sentenceWords) {
            for (int i = startIdx; i < words.size(); i++) {
                String curWord = words.get(i).getWord();
                if (isSimilar(word, curWord)) {
                    startIdx = i + 1;
                    break;
                }
            }
        }
        return startIdx - 1;
    }

    private boolean isSimilar(String word1, String word2) {
        word1 = normalizeText(word1);
        word2 = normalizeText(word2);

        if (word1.equals(word2)) {
            return true;
        }

        int distance = levenshteinDistance(word1, word2);
        int maxLen = Math.max(word1.length(), word2.length());
        return distance <= (maxLen * 0.1);

    }

    private int levenshteinDistance(String word1, String word2) {
        int[][] dp = new int[word1.length() + 1][word2.length() + 1];

        for (int i = 1; i <= word1.length(); i++) {
            for (int j = 1; j <= word2.length(); j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        return dp[word1.length()][word2.length()];
    }

    private String normalizeText(String text) {
        return text.replaceAll("[^\\w\\s]", "")
                .replaceAll("\\s+", "")
                .toLowerCase()
                .trim();
    }
}
