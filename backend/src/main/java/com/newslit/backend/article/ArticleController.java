package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleRequestDto;
import com.newslit.backend.article.dto.ArticleResponseDto;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${spring.application.name}")
    private String publicMessage;

    @Value("${app.secret-message}")
    private String secretMessage;

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


    @GetMapping("/properties")
    public Map<String, String> testProperties() {
        Map<String, String> result = new HashMap<>();

        result.put("public-message", publicMessage);
        result.put("secret-message", secretMessage);
        result.put("status", "Properties loaded successfully!");

        return result;
    }

    @PostMapping
    public ResponseEntity<Article> createArticle(@RequestBody ArticleRequestDto articleRequestDto) {
        Article savedArticle = articleService.saveArticle(articleRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }

}
