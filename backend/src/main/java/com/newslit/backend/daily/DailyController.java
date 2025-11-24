package com.newslit.backend.daily;

import com.newslit.backend.daily.dto.DailyResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/daily")
@Tag(name = "일일 컨텐츠", description = "일일 컨텐츠 관련 API")
public class DailyController {
    private final DailyService dailyService;

    @PostMapping
    public ResponseEntity<List<DailyResponseDto>> split(@RequestParam Long id) {
        List<DailyResponseDto> chunks = dailyService.createChunks(id);
        return ResponseEntity.ok(chunks);
    }

}
