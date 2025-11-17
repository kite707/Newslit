package com.newslit.backend.readingHistory;

import com.newslit.backend.readingHistory.dto.ReadingHistoryResponseDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-history")
public class ReadingHistoryController {
    private final ReadingHistoryService readingHistoryService;

    @GetMapping
    public ResponseEntity<List<ReadingHistoryResponseDto>> getReadingHistory(@RequestParam Long userId,
                                                                             @RequestParam
                                                                             String date) {
        LocalDate parsedDate = LocalDate.parse(date + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<ReadingHistoryResponseDto> historyListResponseDto = readingHistoryService.getReadingHistoryByUserId(userId,
                parsedDate);
        return ResponseEntity.ok(historyListResponseDto);

    }

    @PostMapping
    public ResponseEntity<ReadingHistoryResponseDto> addReadingHistory(
            @CookieValue(name = "userId") Long userId, @RequestParam Long articleId) {
        ReadingHistoryResponseDto responseDto = readingHistoryService.addReadingHistory(userId, articleId);
        return ResponseEntity.ok(responseDto);
    }

}
