package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleResponseDto getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));

        return convertToDto(article);
    }

    private ArticleResponseDto convertToDto(Article article) {
        return ArticleResponseDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .originalText(article.getOriginalText())
                .translatedText(article.getTranslatedText())
                .sourceUrl(article.getSourceUrl())
                .publishedDate(article.getPublishedDate())
                .displayDate(article.getDisplayDate())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
