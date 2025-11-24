package com.newslit.backend.sentence;

import com.deepl.api.DeepLException;
import com.newslit.backend.sentence.dto.SentenceResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sentence")
@Tag(name = "SENTENCE", description = "문장 관련 API")
public class SentenceController {
    private final SentenceService sentenceService;

    @GetMapping
    ResponseEntity<List<SentenceResponseDto>> translateOneParagraph(@RequestParam(name = "articleId") Long articleId)
            throws DeepLException, InterruptedException {
        List<SentenceResponseDto> responses = sentenceService.translateOneParagraph(articleId);

        return ResponseEntity.ok(responses);
    }

}
