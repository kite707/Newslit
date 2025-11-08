package com.newslit.backend.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyResponseDto {

    private Long id;
    private Long articleId;
    private String word;
    private String meaning;
    private String partOfSpeech;
    private String exampleSentence;
    private LocalDateTime createdAt;
}
