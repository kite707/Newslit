package com.newslit.backend.audio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordDto {
    private String word;
    private Double start;
    private Double end;
}
