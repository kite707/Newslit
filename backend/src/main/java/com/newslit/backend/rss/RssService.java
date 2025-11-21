package com.newslit.backend.rss;

import com.newslit.backend.rss.dto.RssResponseDto;
import com.newslit.backend.rss.exception.NoRssFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RssService {
    private final RssRepository rssRepository;

    public List<RssResponseDto> getAllRss() {
        List<Rss> rsses = rssRepository.findAll();
        return rsses.stream().map(this::toDto).toList();
    }

    public RssResponseDto getRssById(Long id) {
        Rss rss = rssRepository.findById(id).orElseThrow(() -> new NoRssFoundException());
        return toDto(rss);
    }

    private RssResponseDto toDto(Rss rss) {
        return RssResponseDto.builder()
                .id(rss.getId())
                .category(rss.getCategory())
                .title(rss.getTitle())
                .url(rss.getUrl())
                .createdAt(rss.getCreatedAt())
                .updatedAt(rss.getUpdatedAt())
                .build();
    }


}
