package com.newslit.backend.vocabulary.dto;

import com.newslit.backend.global.common.enums.PartOfSpeech;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VocabularyRequestDto {
    private Long articleId;
    private String word;
    private PartOfSpeech partOfSpeech;
}
