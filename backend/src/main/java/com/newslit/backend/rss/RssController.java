package com.newslit.backend.rss;

import com.newslit.backend.rss.dto.RssResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rss")
public class RssController {
    private final RssService rssService;

    @GetMapping
    public ResponseEntity<List<RssResponseDto>> getRss() {
        return ResponseEntity.ok(rssService.getAllRss());
    }
}
