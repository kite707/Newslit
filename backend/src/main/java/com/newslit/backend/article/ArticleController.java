package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleAvailableDatesResponseDto;
import com.newslit.backend.article.dto.ArticleRequestDto;
import com.newslit.backend.article.dto.ArticleResponseDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/article")
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDto> getArticleById(@PathVariable Long id) {
        ArticleResponseDto article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }

    @GetMapping
    public ResponseEntity<ArticleResponseDto> getArticleByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {
        ArticleResponseDto article = articleService.getArticleByDate(date);
        return ResponseEntity.ok(article);
    }

    @GetMapping("/available")
    public ResponseEntity<ArticleAvailableDatesResponseDto> getAvailableDates(
            @RequestParam String date) {
        LocalDate parsedDate = LocalDate.parse(date + "01",
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        ArticleAvailableDatesResponseDto responseDto = articleService.getAvailableDates(parsedDate);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    public ResponseEntity<Article> createArticle(@RequestBody ArticleRequestDto articleRequestDto) {
        Article savedArticle = articleService.saveArticle(articleRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }

}
