package com.newslit.backend.readingHistory;

import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.daily.Daily;
import com.newslit.backend.daily.DailyRespository;
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
    private final DailyRespository dailyRespository;

    public List<ReadingHistoryResponseDto> getReadingHistoryByUserId(Long id, LocalDate date) {

        Optional<List<ReadingHistory>> histories = readingHistoryRepository.findAllByUserIdAndDisplayDateBetween(id,
                date,
                date.plusMonths(1).minusDays(1));

        List<ReadingHistoryResponseDto> historyDtos = histories
                .orElse(Collections.emptyList())
                .stream()
                .map(this::convertToDto)
                .toList();

        return historyDtos;
    }

    @Transactional
    public ReadingHistoryResponseDto addReadingHistory(Long id, Long dailyId) {
        Daily daily = dailyRespository.findById(dailyId).orElseThrow(() -> new ArticleNotFoundException());
        ReadingHistory readingHistory = new ReadingHistory();
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException());

        readingHistory.setDaily(daily);
        readingHistory.setDisplayDate(daily.getDisplayDate());
        readingHistory.setUpdatedAt(LocalDateTime.now());
        readingHistory.setUser(user);
        ReadingHistory save = readingHistoryRepository.save(readingHistory);
        return convertToDto(save);
    }

    private ReadingHistoryResponseDto convertToDto(ReadingHistory history) {
        return ReadingHistoryResponseDto.builder()
                .displayDate(history.getDisplayDate())
                .dailyId(history.getDaily().getId())
                .articleId(history.getDaily().getArticle().getId())
                .userId(history.getUser().getId())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .id(history.getId())
                .build();
    }
}
