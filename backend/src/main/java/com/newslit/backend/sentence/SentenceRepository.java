package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {
    interface TranslationCandidate {
        Long getSentenceId();

        Long getArticleId();
    }

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.article.pipelineStage = :pipelineStage
              AND s.article.pipelineStatus IN :pipelineStatuses
              AND s.translationStatus IN :translationStatuses
              AND s.translationRetryCount < :maxRetryCount
            ORDER BY s.id
            """)
    List<TranslationCandidate> findTranslationCandidates(
            @Param("pipelineStage") com.newslit.backend.article.ArticlePipelineStage pipelineStage,
            @Param("pipelineStatuses") List<Status> pipelineStatuses,
            @Param("translationStatuses") List<Status> translationStatuses,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.article.id = :articleId
              AND s.article.pipelineStage = :pipelineStage
              AND s.article.pipelineStatus IN :pipelineStatuses
              AND s.translationStatus IN :translationStatuses
              AND s.translationRetryCount < :maxRetryCount
            ORDER BY s.id
            """)
    List<TranslationCandidate> findTranslationCandidatesByArticleId(
            @Param("articleId") Long articleId,
            @Param("pipelineStage") com.newslit.backend.article.ArticlePipelineStage pipelineStage,
            @Param("pipelineStatuses") List<Status> pipelineStatuses,
            @Param("translationStatuses") List<Status> translationStatuses,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.translationStatus = :processingStatus,
                s.updatedAt = :claimedAt
            WHERE s.id = :sentenceId
              AND s.article.pipelineStage = :pipelineStage
              AND s.article.pipelineStatus IN :pipelineStatuses
              AND s.translationStatus IN :claimableStatuses
              AND s.translationRetryCount < :maxRetryCount
            """)
    int claimTranslation(@Param("sentenceId") Long sentenceId,
                         @Param("pipelineStage") com.newslit.backend.article.ArticlePipelineStage pipelineStage,
                         @Param("pipelineStatuses") List<Status> pipelineStatuses,
                         @Param("claimableStatuses") List<Status> claimableStatuses,
                         @Param("processingStatus") Status processingStatus,
                         @Param("maxRetryCount") int maxRetryCount,
                         @Param("claimedAt") LocalDateTime claimedAt);

    @Query("""
            SELECT s
            FROM Sentence s
            JOIN FETCH s.article
            WHERE s.id = :sentenceId
              AND s.translationStatus = :status
            """)
    Optional<Sentence> findTranslationWork(@Param("sentenceId") Long sentenceId,
                                           @Param("status") Status status);

    long countByArticleIdAndTranslationStatusNot(Long articleId, Status status);

    long countByArticleId(Long articleId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.translationRetryCount = 0
            WHERE s.article.pipelineStage = :initialStage
              AND s.article.pipelineStatus = :pendingStatus
              AND s.translationStatus <> :successStatus
            """)
    int resetLegacyTranslationRetryCounts(
            @Param("initialStage") com.newslit.backend.article.ArticlePipelineStage initialStage,
            @Param("pendingStatus") Status pendingStatus,
            @Param("successStatus") Status successStatus);

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.article.pipelineStage = :pipelineStage
              AND s.article.pipelineStatus IN :pipelineStatuses
              AND s.translationStatus = :processingStatus
              AND s.translationRetryCount < :maxRetryCount
              AND (s.updatedAt IS NULL OR s.updatedAt < :threshold)
            ORDER BY s.updatedAt, s.id
            """)
    List<TranslationCandidate> findStaleTranslationCandidates(
            @Param("pipelineStage") com.newslit.backend.article.ArticlePipelineStage pipelineStage,
            @Param("pipelineStatuses") List<Status> pipelineStatuses,
            @Param("processingStatus") Status processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.translationStatus = :failedStatus,
                s.translationRetryCount = s.translationRetryCount + 1,
                s.updatedAt = :updatedAt
            WHERE s.id = :sentenceId
              AND s.article.pipelineStage = :pipelineStage
              AND s.article.pipelineStatus IN :pipelineStatuses
              AND s.translationStatus = :processingStatus
              AND s.translationRetryCount < :maxRetryCount
              AND (s.updatedAt IS NULL OR s.updatedAt < :threshold)
            """)
    int recoverStaleTranslation(@Param("sentenceId") Long sentenceId,
                                @Param("pipelineStage") com.newslit.backend.article.ArticlePipelineStage pipelineStage,
                                @Param("pipelineStatuses") List<Status> pipelineStatuses,
                                @Param("threshold") LocalDateTime threshold,
                                @Param("failedStatus") Status failedStatus,
                                @Param("processingStatus") Status processingStatus,
                                @Param("maxRetryCount") int maxRetryCount,
                                @Param("updatedAt") LocalDateTime updatedAt);

    List<Sentence> findAllByArticleId(Long id);

    List<Sentence> findAllByArticleIdOrderByOrderIndexAsc(Long articleId);

    List<Sentence> findAllByArticleIdAndOrderIndexBetween(Long id, int startIndex, int endIndex);

}
