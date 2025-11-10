package com.newslit.backend.readingHistory;

import com.newslit.backend.readingHistory.dto.ReadingHistoryResponseDto;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadingHistoryService {
    private final ReadingHistoryRepository readingHistoryRepository;

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

    private ReadingHistoryResponseDto convertToDto(ReadingHistory history) {
        return ReadingHistoryResponseDto.builder()
                .articleId(history.getArticleId())
                .readDate(history.getReadDate())
                .userId(history.getUserId())
                .createdAt(history.getCreatedAt())
                .id(history.getId())
                .build();
    }


}
