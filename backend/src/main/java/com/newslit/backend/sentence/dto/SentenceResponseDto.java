package com.newslit.backend.sentence.dto;

import com.newslit.backend.global.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SentenceResponseDto {
    private Long articleId;
    private Double startTime;
    private Double endTime;
    private int orderIndex;
    private String englishText;
    private String koreanText;
    private Status status;
}
