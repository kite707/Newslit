package com.newslit.backend.vocabulary;


import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.vocabulary.dto.VocabularyRequestDto;
import com.newslit.backend.vocabulary.dto.VocabularyResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;
    private final ArticleRepository articleRepository;

    public List<VocabularyResponseDto> findByArticleId(Long id) {
        List<Vocabulary> vocabularyList = vocabularyRepository.findAllByArticleId(id);
        return vocabularyList.stream().map(this::toDto).toList();

    }

    public Vocabulary saveVocabulary(VocabularyRequestDto vocabularyRequestDto) {
        Article article = articleRepository.findById(vocabularyRequestDto.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article not found"));
        Vocabulary vocabulary = Vocabulary.builder()
                .article(article)
                .word(vocabularyRequestDto.getWord())
                .partOfSpeech(vocabularyRequestDto.getPartOfSpeech())
                .build();
        return vocabularyRepository.save(vocabulary);
    }

    private VocabularyResponseDto toDto(Vocabulary vocabulary) {
        return VocabularyResponseDto.builder()
                .id(vocabulary.getId())
                .articleId(vocabulary.getArticle().getId())
                .word(vocabulary.getWord())
                .meaning(vocabulary.getMeaning())
                .partOfSpeech(vocabulary.getPartOfSpeech().getKorean())
                .exampleSentence(vocabulary.getExampleSentence())
                .exampleTranslation(vocabulary.getExampleTranslation())
                .createdAt(vocabulary.getCreatedAt())
                .build();
    }


}
