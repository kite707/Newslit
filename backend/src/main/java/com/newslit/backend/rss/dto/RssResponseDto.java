package com.newslit.backend.rss.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RssResponseDto {
    private Long id;
    private String category;
    private String title;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
