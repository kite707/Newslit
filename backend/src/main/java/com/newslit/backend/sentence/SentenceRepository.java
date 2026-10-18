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
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.translationRetryCount = 0,
                s.updatedAt = :updatedAt
            WHERE s.article.id = :articleId
              AND s.translationStatus = :failedStatus
            """)
    int resetFailedTranslationRetryCounts(@Param("articleId") Long articleId,
                                          @Param("failedStatus") Status failedStatus,
                                          @Param("updatedAt") LocalDateTime updatedAt);

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

    interface TtsCandidate {
        Long getSentenceId();

        Long getArticleId();
    }

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.article.pipelineStage = :completedStage
              AND s.article.pipelineStatus = :successStatus
              AND s.ttsStatus IN :ttsStatuses
              AND s.ttsRetryCount < :maxRetryCount
            ORDER BY s.article.publishedDate DESC, s.id
            """)
    List<TtsCandidate> findTtsCandidates(
            @Param("completedStage") com.newslit.backend.article.ArticlePipelineStage completedStage,
            @Param("successStatus") Status successStatus,
            @Param("ttsStatuses") List<Status> ttsStatuses,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.article.id = :articleId
              AND s.ttsStatus IN :ttsStatuses
              AND s.ttsRetryCount < :maxRetryCount
            ORDER BY s.orderIndex
            """)
    List<TtsCandidate> findTtsCandidatesByArticleId(
            @Param("articleId") Long articleId,
            @Param("ttsStatuses") List<Status> ttsStatuses,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.ttsStatus = :processingStatus,
                s.updatedAt = :claimedAt
            WHERE s.id = :sentenceId
              AND s.ttsStatus IN :claimableStatuses
              AND s.ttsRetryCount < :maxRetryCount
            """)
    int claimTts(@Param("sentenceId") Long sentenceId,
                 @Param("claimableStatuses") List<Status> claimableStatuses,
                 @Param("processingStatus") Status processingStatus,
                 @Param("maxRetryCount") int maxRetryCount,
                 @Param("claimedAt") LocalDateTime claimedAt);

    @Query("""
            SELECT s
            FROM Sentence s
            JOIN FETCH s.article
            WHERE s.id = :sentenceId
              AND s.ttsStatus = :status
            """)
    Optional<Sentence> findTtsWork(@Param("sentenceId") Long sentenceId,
                                   @Param("status") Status status);

    @Query("""
            SELECT s.id AS sentenceId, s.article.id AS articleId
            FROM Sentence s
            WHERE s.ttsStatus = :processingStatus
              AND s.ttsRetryCount < :maxRetryCount
              AND (s.updatedAt IS NULL OR s.updatedAt < :threshold)
            ORDER BY s.updatedAt, s.id
            """)
    List<TtsCandidate> findStaleTtsCandidates(
            @Param("processingStatus") Status processingStatus,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.ttsStatus = :failedStatus,
                s.ttsRetryCount = s.ttsRetryCount + 1,
                s.updatedAt = :updatedAt
            WHERE s.id = :sentenceId
              AND s.ttsStatus = :processingStatus
              AND s.ttsRetryCount < :maxRetryCount
              AND (s.updatedAt IS NULL OR s.updatedAt < :threshold)
            """)
    int recoverStaleTts(@Param("sentenceId") Long sentenceId,
                        @Param("threshold") LocalDateTime threshold,
                        @Param("failedStatus") Status failedStatus,
                        @Param("processingStatus") Status processingStatus,
                        @Param("maxRetryCount") int maxRetryCount,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Sentence s
            SET s.ttsRetryCount = 0,
                s.updatedAt = :updatedAt
            WHERE s.article.id = :articleId
              AND s.ttsStatus = :failedStatus
            """)
    int resetFailedTtsRetryCounts(@Param("articleId") Long articleId,
                                  @Param("failedStatus") Status failedStatus,
                                  @Param("updatedAt") LocalDateTime updatedAt);
}
