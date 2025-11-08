package com.newslit.backend.readingHistory;

import com.newslit.backend.readingHistory.dto.ReadingHistoryListResponseDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-history")
public class ReadingHistoryController {
    private final ReadingHistoryService readingHistoryService;

    @GetMapping
    public ResponseEntity<ReadingHistoryListResponseDto> getReadingHistory(@RequestParam Long userId,
                                                                           @RequestParam
                                                                           String date) {
        LocalDate parsedDate = LocalDate.parse(date + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        ReadingHistoryListResponseDto historyListResponseDto = readingHistoryService.getReadingHistoryByUserId(userId,
                parsedDate);
        return ResponseEntity.ok(historyListResponseDto);

    }
}
