package com.newslit.backend.vocabulary;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findAllByArticleId(Long id);

    List<Vocabulary> findAllByMeaningIsNull();
}
