package com.newslit.backend.daily.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyResponseDto {
    private Long articleId;
    private int startIndex;
    private int endIndex;
    private int wordCount;
}
