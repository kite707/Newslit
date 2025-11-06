package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<ArticleResponseDto> getArticle(@PathVariable Long id){
        ArticleResponseDto article = articleService.getArticle(id);
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

}
