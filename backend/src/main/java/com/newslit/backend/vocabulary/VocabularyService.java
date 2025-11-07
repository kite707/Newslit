package com.newslit.backend.vocabulary;


import com.newslit.backend.vocabulary.dto.VocabularyResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VocabularyService {
    private final VocabularyRepository vocabularyRepository;

    public List<VocabularyResponseDto> findByArticleId(Long id){
        List<Vocabulary> vocabularyList = vocabularyRepository.findAllByArticleId(id);
        return vocabularyList.stream().map(this::toDto).toList();

    }

    private VocabularyResponseDto toDto(Vocabulary vocabulary){
        return VocabularyResponseDto.builder()
                .id(vocabulary.getId())
                .articleId(vocabulary.getArticleId())
                .word(vocabulary.getWord())
                .meaning(vocabulary.getMeaning())
                .partOfSpeech(vocabulary.getPartOfSpeech())
                .exampleSentence(vocabulary.getExampleSentence())
                .createdAt(vocabulary.getCreatedAt())
                .build();
    }


}
