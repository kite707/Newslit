package com.newslit.backend.sentence;

import com.newslit.backend.sentence.dto.SentenceResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sentence")
@Tag(name = "SENTENCE", description = "문장 관련 API")
public class SentenceController {
    private final SentenceService sentenceService;

    @PostMapping("/translate")
    ResponseEntity<Void> triggerTranslation(@RequestParam(name = "articleId") Long articleId) {
        sentenceService.triggerTranslation(articleId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    ResponseEntity<List<SentenceResponseDto>> getSentences(@RequestParam(name = "articleId") Long articleId) {
        return ResponseEntity.ok(sentenceService.getSentences(articleId));
    }

}
