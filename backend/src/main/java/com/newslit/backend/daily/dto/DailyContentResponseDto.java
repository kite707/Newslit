package com.newslit.backend.daily.dto;

import com.newslit.backend.sentence.dto.SentenceResponseDto;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyContentResponseDto {
    private String title;
    private LocalDate publishedDate;
    private String source;
    private String sourceUrl;
    private List<SentenceResponseDto> sentences;
}
