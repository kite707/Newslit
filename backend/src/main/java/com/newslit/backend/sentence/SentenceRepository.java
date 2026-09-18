package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    List<Sentence> findAllByTranslationStatusAndRetryCountLessThan(Status status, int count);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.translationStatus = :failedStatus
            WHERE s.translationStatus = :processingStatus
              AND (s.updatedAt IS NULL OR s.updatedAt < :threshold)
            """)
    int markStaleProcessingAsFailed(@Param("threshold") LocalDateTime threshold,
                                    @Param("failedStatus") Status failedStatus,
                                    @Param("processingStatus") Status processingStatus);

    List<Sentence> findAllByArticleId(Long id);

    List<Sentence> findAllByArticleIdAndOrderIndexBetween(Long id, int startIndex, int endIndex);

    Optional<Sentence> findByArticleIdAndEnglishText(Long articleId, String englishText);


}
