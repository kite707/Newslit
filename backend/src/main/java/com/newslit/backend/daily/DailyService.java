package com.newslit.backend.daily;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.daily.dto.DailyContentResponseDto;
import com.newslit.backend.daily.dto.DailyResponseDto;
import com.newslit.backend.daily.exception.DailyChunkCreationException;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyService {
    private final DailyRespository dailyRespository;
    private final SentenceRepository sentenceRepository;
    private final ArticleRepository articleRepository;

    private static final int MIN_WORDS = 100;
    private static final int MAX_WORDS = 150;
    private static final int TARGET_WORDS = 125;

    @Transactional
    public List<DailyResponseDto> createChunks(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new ArticleNotFoundException());
        List<Sentence> sentences = sentenceRepository.findAllByArticleIdOrderByOrderIndexAsc(id).stream()
                .sorted(Comparator.comparing(Sentence::getOrderIndex))
                .toList();
        validateChunkableSentences(sentences);

        int[] wordCounts = new int[sentences.size()];
        int totalWords = 0;
        for (int i = 0; i < sentences.size(); i++) {
            wordCounts[i] = countWords(sentences.get(i).getEnglishText());
            totalWords += wordCounts[i];
        }

        if (totalWords == 0) {
            throw new DailyChunkCreationException("Article " + id + "의 단어 수를 계산할 수 없습니다");
        }

        int targetChunkCount = calculateChunkCount(totalWords);
        int targetWordsPerChunk = totalWords / targetChunkCount;
        List<ChunkRange> chunkRanges = calculateChunkRanges(sentences, wordCounts, targetWordsPerChunk);
        List<DailyResponseDto> responses = new ArrayList<>();

        for (ChunkRange range : chunkRanges) {
            Daily daily = dailyRespository
                    .findByArticleIdAndStartIndexAndEndIndex(article.getId(), range.startIndex(), range.endIndex())
                    .orElseGet(() -> saveDaily(article, range));
            responses.add(toDailyDto(daily));
        }

        return responses;
    }

    private List<ChunkRange> calculateChunkRanges(
            List<Sentence> sentences,
            int[] wordCounts,
            int targetWordsPerChunk
    ) {
        List<ChunkRange> ranges = new ArrayList<>();
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
                int startOrderIndex = sentences.get(chunkStart).getOrderIndex();
                int endOrderIndex = sentences.get(i).getOrderIndex();
                ranges.add(new ChunkRange(startOrderIndex, endOrderIndex, currentWordCount));
                chunkStart = i + 1;
                currentWordCount = 0;
            }
        }
        return ranges;
    }

    private Daily saveDaily(Article article, ChunkRange range) {
        LocalDate displayDate = dailyRespository.findTopByOrderByDisplayDateDesc()
                .map(latestDaily -> latestDaily.getDisplayDate().plusDays(1))
                .orElse(LocalDate.now());

        return dailyRespository.saveAndFlush(Daily.builder()
                .startIndex(range.startIndex())
                .endIndex(range.endIndex())
                .wordCount(range.wordCount())
                .article(article)
                .displayDate(displayDate)
                .build());
    }

    private void validateChunkableSentences(List<Sentence> sentences) {
        if (sentences.isEmpty()) {
            throw new DailyChunkCreationException("문장이 없어 일일 컨텐츠를 생성할 수 없습니다");
        }

        boolean hasIncompleteTranslation = sentences.stream()
                .anyMatch(sentence -> sentence.getTranslationStatus() != Status.SUCCESS);
        if (hasIncompleteTranslation) {
            throw new DailyChunkCreationException("번역이 완료되지 않은 문장이 있습니다");
        }
    }

    public DailyContentResponseDto getDailyContent(LocalDate displayDate) {
        Daily daily = dailyRespository.findByDisplayDate(displayDate);

        Long articleId = daily.getArticle().getId();
        int startIndex = daily.getStartIndex();
        int endIndex = daily.getEndIndex();

        List<SentenceResponseDto> sentences = sentenceRepository.findAllByArticleIdAndOrderIndexBetween(articleId,
                startIndex,
                endIndex).stream().map(this::toSentenceDto).collect(Collectors.toList());

        List<Daily> dailyList = dailyRespository.findByArticleIdOrderByIdAsc(articleId);
        int totalPages = dailyList.size();

        int currentPage = dailyList.indexOf(daily) + 1;

        return toDailyContentDto(daily, sentences, totalPages, currentPage);
    }

    public List<DailyResponseDto> getAvailableDate(LocalDate date) {
        LocalDate startDate = date;
        LocalDate endDate = date.plusMonths(1).minusDays(1);

        return dailyRespository.findAllByDisplayDateBetween(startDate, endDate)
                .stream().map(this::toDailyDto).collect(
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
                .audioUrl(daily.getArticle().getAudioLink())
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
                .startTime(sentence.getStartTime())
                .endTime(sentence.getEndTime())
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

    private record ChunkRange(int startIndex, int endIndex, int wordCount) {
    }

}
