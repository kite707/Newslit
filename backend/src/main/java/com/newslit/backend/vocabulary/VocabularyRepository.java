package com.newslit.backend.vocabulary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findAllByArticleId(Long id);

    List<Vocabulary> findAllByMeaningIsNull();

    Optional<Vocabulary> findByArticleIdAndWord(Long id, String word);
}
