package com.newslit.backend.article;

import com.newslit.backend.article.dto.ArticleResponseDto;
import com.newslit.backend.vocabulary.VocabularyService;
import com.newslit.backend.vocabulary.dto.VocabularyResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final VocabularyService vocabularyService;

    public ArticleResponseDto getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + id));
        List<VocabularyResponseDto> vocabularies = vocabularyService.findByArticleId(id);

        return convertToDto(article,vocabularies);
    }

    private ArticleResponseDto convertToDto(Article article, List<VocabularyResponseDto>vocabularies) {
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
