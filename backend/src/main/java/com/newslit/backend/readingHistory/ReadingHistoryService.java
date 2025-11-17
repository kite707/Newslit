package com.newslit.backend.readingHistory;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.readingHistory.dto.ReadingHistoryResponseDto;
import com.newslit.backend.user.User;
import com.newslit.backend.user.UserRepository;
import com.newslit.backend.user.exception.UserNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {
    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;

    public List<ReadingHistoryResponseDto> getReadingHistoryByUserId(Long id, LocalDate date) {

        Optional<List<ReadingHistory>> histories = readingHistoryRepository.findAllByUserIdAndReadDateBetween(id, date,
                date.plusMonths(1).minusDays(1));

        List<ReadingHistoryResponseDto> historyDtos = histories
                .orElse(Collections.emptyList())
                .stream()
                .map(this::convertToDto)
                .toList();

        return historyDtos;
    }

    @Transactional
    public ReadingHistoryResponseDto addReadingHistory(Long id, Long articleId) {
        ReadingHistory readingHistory = readingHistoryRepository.findByUserIdAndArticleId(id, articleId)
                .orElse(new ReadingHistory());
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException());
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new ArticleNotFoundException());
        readingHistory.setUser(user);

        readingHistory.setArticle(article);
        readingHistory.setReadDate(LocalDate.now());
        readingHistory.setUpdatedAt(LocalDateTime.now());
        ReadingHistory save = readingHistoryRepository.save(readingHistory);
        return convertToDto(save);
    }

    private ReadingHistoryResponseDto convertToDto(ReadingHistory history) {
        return ReadingHistoryResponseDto.builder()
                .readDate(history.getReadDate())
                .articleId(history.getArticle().getId())
                .userId(history.getUser().getId())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .id(history.getId())
                .build();
    }
}
