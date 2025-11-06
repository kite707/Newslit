package com.newslit.backend.article.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ArticleResponseDto.java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponseDto {
    private Long id;
    private String title;
    private String originalText;
    private String translatedText;
    private String sourceUrl;
    private LocalDate publishedDate;
    private LocalDate displayDate;
    private LocalDateTime createdAt;
//    private List<SentenceResponseDto> sentences;
//    private List<VocabularyResponseDto> vocabularies;
}
