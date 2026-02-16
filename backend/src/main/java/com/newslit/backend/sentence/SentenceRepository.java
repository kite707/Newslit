package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    List<Sentence> findAllByTranslationStatusAndRetryCountLessThan(Status status, int count);

    List<Sentence> findAllByArticleId(Long id);

    List<Sentence> findAllByArticleIdAndOrderIndexBetween(Long id, int startIndex, int endIndex);

    Optional<Sentence> findByArticleIdAndEnglishText(Long articleId, String englishText);


}
