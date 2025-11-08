package com.newslit.backend.readingHistory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingHistoryResponseDto {
    private Long id;
    private Long userId;
    private Long articleId;
    private LocalDate readDate;
    private LocalDateTime createdAt;
}
