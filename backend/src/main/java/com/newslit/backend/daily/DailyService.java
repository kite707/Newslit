package com.newslit.backend.daily;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.daily.dto.DailyContentResponseDto;
import com.newslit.backend.daily.dto.DailyResponseDto;
import com.newslit.backend.daily.exception.DuplicateDailyException;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyService {
    private final DailyRespository dailyRespository;
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    private static final int MIN_WORDS = 100;
    private static final int MAX_WORDS = 150;
    private static final int TARGET_WORDS = 125;

    public List<DailyResponseDto> createChunks(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new ArticleNotFoundException());
        List<Sentence> sentences = sentenceRepository.findAllByArticleId(id);
        List<DailyResponseDto> dailyResponseDto = new ArrayList<>();

        int[] wordCounts = new int[sentences.size()];
        int totalWords = 0;
        for (int i = 0; i < sentences.size(); i++) {
            wordCounts[i] = countWords(sentences.get(i).getEnglishText());
            totalWords += wordCounts[i];
        }

        int targetChunkCount = calculateChunkCount(totalWords);
        int targetWordsPerChunk = totalWords / targetChunkCount;

        int chunkStart = 0;
        int currentWordCount = 0;

        for (int i = 0; i < sentences.size(); i++) {
            currentWordCount += wordCounts[i];

            boolean shouldSplit = false;

            if (i == sentences.size() - 1) {
                shouldSplit = true;
            } else {
                int nextWordCount = wordCounts[i + 1];

                int remainingWords = 0;
                for (int j = i + 1; j < sentences.size(); j++) {
                    remainingWords += wordCounts[j];
                }

                if (currentWordCount + nextWordCount > MAX_WORDS) {
                    shouldSplit = currentWordCount >= MIN_WORDS;
                } else if (currentWordCount >= MIN_WORDS) {
                    if (remainingWords >= MIN_WORDS) {
                        int distanceNow = Math.abs(currentWordCount - targetWordsPerChunk);
                        int distanceNext = Math.abs(currentWordCount + nextWordCount - targetWordsPerChunk);
                        shouldSplit = distanceNext > distanceNow;
                    } else {
                        shouldSplit = false;
                    }
                }
            }

            if (shouldSplit) {
                LocalDate dateToSave = dailyRespository.findTopByOrderByDisplayDateDesc()
                        .map(latestDaily -> latestDaily.getDisplayDate().plusDays(1))
                        .orElse(LocalDate.now());

                Daily daily = Daily.builder()
                        .startIndex(chunkStart)
                        .endIndex(i)
                        .wordCount(currentWordCount)
                        .article(article)
                        .displayDate(dateToSave)
                        .build();

                dailyRespository.findByArticleIdAndStartIndexAndEndIndex(article.getId(), chunkStart, i)
                        .ifPresent(k -> {
                            throw new DuplicateDailyException();
                        });
                dailyRespository.save(daily);
                dailyResponseDto.add(toDailyDto(daily));
                chunkStart = i + 1;
                currentWordCount = 0;
            }
        }
        return dailyResponseDto;
    }

    public DailyContentResponseDto getDailyContent(LocalDate displayDate) {
        Daily daily = dailyRespository.findByDisplayDate(displayDate);

        Long articleId = daily.getArticle().getId();
        int startIndex = daily.getStartIndex();
        int endIndex = daily.getEndIndex();

        List<SentenceResponseDto> sentences = sentenceRepository.findAllByArticleIdAndOrderIndexBetween(articleId,
                startIndex,
                endIndex).stream().map(sentence -> toSentenceDto(sentence)).collect(Collectors.toList());

        List<Daily> dailyList = dailyRespository.findByArticleIdOrderByIdAsc(articleId);
        int totalPages = dailyList.size();

        int currentPage = dailyList.indexOf(daily) + 1;
        DailyContentResponseDto dailyContentResponseDto = toDailyContentDto(daily, sentences, totalPages, currentPage);

        return dailyContentResponseDto;
    }

    public List<DailyResponseDto> getAvailableDate(LocalDate date) {
        LocalDate startDate = date;
        LocalDate endDate = date.plusMonths(1).minusDays(1);

        return dailyRespository.findAllByDisplayDateBetween(startDate, endDate)
                .stream().map(daily -> toDailyDto(daily)).collect(
                        Collectors.toList());
    }

    private int calculateChunkCount(int totalWords) {
        if (totalWords <= MAX_WORDS) {
            return 1;
        }

        int count = Math.round((float) totalWords / TARGET_WORDS);

        while (count > 1 && totalWords / count < MIN_WORDS) {
            count--;
        }

        return Math.max(1, count);
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private DailyResponseDto toDailyDto(Daily daily) {
        return DailyResponseDto.builder()
                .articleId(daily.getArticle().getId())
                .startIndex(daily.getStartIndex())
                .endIndex(daily.getEndIndex())
                .wordCount(daily.getWordCount())
                .displayDate(daily.getDisplayDate())
                .build();
    }

    private SentenceResponseDto toSentenceDto(Sentence sentence) {
        return SentenceResponseDto.builder()
                .articleId(sentence.getArticle().getId())
                .englishText(sentence.getEnglishText())
                .koreanText(sentence.getKoreanText())
                .orderIndex(sentence.getOrderIndex())
                .status(sentence.getTranslationStatus())
                .build();
    }

    private DailyContentResponseDto toDailyContentDto(Daily daily, List<SentenceResponseDto> sentences, int totalPages,
                                                      int currentPages) {
        return DailyContentResponseDto.builder()
                .dailyId(daily.getId())
                .title(daily.getArticle().getTitle())
                .publishedDate(daily.getArticle().getPublishedDate())
                .source(daily.getArticle().getSource())
                .sourceUrl(daily.getArticle().getSourceUrl())
                .sentences(sentences)
                .totalPages(totalPages)
                .currentPages(currentPages)
                .build();
    }

}