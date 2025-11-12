package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleRequestDto;
import com.newslit.backend.article.dto.ArticleResponseDto;
import com.newslit.backend.article.exception.DuplicateArticleException;
import com.newslit.backend.vocabulary.VocabularyService;
import com.newslit.backend.vocabulary.dto.VocabularyResponseDto;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final VocabularyService vocabularyService;

    public ArticleResponseDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        List<VocabularyResponseDto> vocabularies = vocabularyService.findByArticleId(id);

        return convertToDto(article, vocabularies);
    }

    public ArticleResponseDto getArticleByDate(LocalDate date) {
        Article article = articleRepository.findArticleByDisplayDate(date)
                .orElseThrow(() -> new RuntimeException("Article not found with date: " + date));
        List<VocabularyResponseDto> vocabularies = vocabularyService.findByArticleId(article.getId());

        return convertToDto(article, vocabularies);
    }

    @Transactional
    public Article saveArticle(ArticleRequestDto articleRequestDto) {
        articleRepository.findByTitleAndSource(articleRequestDto.getTitle(),
                articleRequestDto.getSource()).ifPresent(article -> {
            throw new DuplicateArticleException("Article already exist : " + articleRequestDto.getTitle());
        });
        Article article = Article.builder().title(articleRequestDto.getTitle())
                .originalText(articleRequestDto.getOriginalText())
                .sourceUrl(articleRequestDto.getSourceUrl())
                .publishedDate(articleRequestDto.getPublishedDate())
                .source(articleRequestDto.getSource()).build();
        return articleRepository.save(article);
    }

    private ArticleResponseDto convertToDto(Article article, List<VocabularyResponseDto> vocabularies) {
        return ArticleResponseDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .originalText(article.getOriginalText())
                .translatedText(article.getTranslatedText())
                .sourceUrl(article.getSourceUrl())
                .publishedDate(article.getPublishedDate())
                .displayDate(article.getDisplayDate())
                .createdAt(article.getCreatedAt())
                .source(article.getSource())
                .vocabularies(vocabularies)
                .build();
    }
}
