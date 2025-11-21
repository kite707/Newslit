package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleRequestDto;
import com.newslit.backend.article.dto.ArticleResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/article")
@Tag(name = "기사", description = "기사 관련 API")
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponseDto> getArticleById(@PathVariable Long id) {
        ArticleResponseDto article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }


    @PostMapping
    public ResponseEntity<Article> createArticle(@RequestBody ArticleRequestDto articleRequestDto) {
        Article savedArticle = articleService.saveArticle(articleRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }

}
