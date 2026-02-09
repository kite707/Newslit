package com.newslit.backend.audio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhisperResponseDto {
    private String text;
    private String language;
    private Double duration;
    private List<SegmentDto> segments;
}
