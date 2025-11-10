package com.newslit.backend.article.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArticleRequestDto {
    private String title;
    private String originalText;
    private String sourceUrl;
    private LocalDate publishedDate;
    private String source;
}
