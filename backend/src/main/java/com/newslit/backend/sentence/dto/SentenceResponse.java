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
public class SentenceResponse {
    private Long articleId;
    private int orderIdx;
    private String englishText;
    private String koreanText;
    private Status status;
}
