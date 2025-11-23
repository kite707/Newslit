package com.newslit.backend.rss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RssRequestDto {
    private String category;
    private String title;
    private String url;
}
