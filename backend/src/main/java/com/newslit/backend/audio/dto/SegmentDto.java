package com.newslit.backend.audio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SegmentDto {
    private Integer id;
    private Double start;
    private Double end;
    private String text;
}
