package com.newslit.backend.vocabulary;

import com.newslit.backend.vocabulary.dto.VocabularyRequestDto;
import com.newslit.backend.vocabulary.dto.VocabularyResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vocabulary")
@Tag(name = "단어", description = "단어 관련 API")
public class VocabularyController {
    private final VocabularyService vocabularyService;

    @PostMapping
    public ResponseEntity<Vocabulary> createVocabulary(
            @RequestBody VocabularyRequestDto vocabularyRequestDto) {
        Vocabulary savedVocabulary = vocabularyService.saveVocabulary(vocabularyRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVocabulary);
    }

    @PostMapping("/translate")
    public ResponseEntity<List<Vocabulary>> translateVocabulary() {
        List<Vocabulary> vocabularyList = vocabularyService.translateVocabulary();
        return ResponseEntity.status(HttpStatus.CREATED).body(vocabularyList);
    }

    @GetMapping
    public ResponseEntity<List<VocabularyResponseDto>> getArticleVocabulary(@RequestParam Long articleId) {
        List<VocabularyResponseDto> articleVocabulary = vocabularyService.findByArticleId(articleId);
        return ResponseEntity.ok(articleVocabulary);
    }

}
