package com.newslit.backend.readingHistory.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingHistoryListResponseDto {
    private List<ReadingHistoryResponseDto> histories;
}
