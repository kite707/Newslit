package com.newslit.backend.article;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("""
            SELECT a.id
            FROM Article a
            WHERE a.pipelineStage = :translationStage
              AND a.pipelineStatus IN :activeStatuses
              AND EXISTS (
                    SELECT s.id
                    FROM Sentence s
                    WHERE s.article = a
              )
              AND NOT EXISTS (
                    SELECT s.id
                    FROM Sentence s
                    WHERE s.article = a
                      AND s.translationStatus <> :successStatus
              )
            ORDER BY a.pipelineUpdatedAt, a.id
            """)
    List<Long> findTranslationCompletedArticleIds(
            @Param("translationStage") ArticlePipelineStage translationStage,
            @Param("activeStatuses") List<Status> activeStatuses,
            @Param("successStatus") Status successStatus,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStage = :completedStage,
                a.pipelineStatus = :successStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.pipelineStage = :initialStage
              AND a.pipelineStatus = :pendingStatus
              AND EXISTS (
                    SELECT d.id
                    FROM Daily d
                    WHERE d.article = a
              )
            """)
    int backfillLegacyCompletedArticles(
            @Param("initialStage") ArticlePipelineStage initialStage,
            @Param("completedStage") ArticlePipelineStage completedStage,
            @Param("pendingStatus") Status pendingStatus,
            @Param("successStatus") Status successStatus,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStage = :dailyChunkStage,
                a.pipelineStatus = :pendingStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.pipelineStage = :initialStage
              AND a.pipelineStatus = :pendingStatus
              AND EXISTS (
                    SELECT s.id
                    FROM Sentence s
                    WHERE s.article = a
              )
              AND NOT EXISTS (
                    SELECT s.id
                    FROM Sentence s
                    WHERE s.article = a
                      AND s.translationStatus <> :successStatus
              )
            """)
    int backfillLegacyDailyChunkArticles(
            @Param("initialStage") ArticlePipelineStage initialStage,
            @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
            @Param("pendingStatus") Status pendingStatus,
            @Param("successStatus") Status successStatus,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStage = :translationStage,
                a.pipelineStatus = :pendingStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.pipelineStage = :initialStage
              AND a.pipelineStatus = :pendingStatus
              AND EXISTS (
                    SELECT s.id
                    FROM Sentence s
                    WHERE s.article = a
              )
            """)
    int backfillLegacyTranslationArticles(
            @Param("initialStage") ArticlePipelineStage initialStage,
            @Param("translationStage") ArticlePipelineStage translationStage,
            @Param("pendingStatus") Status pendingStatus,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
            SELECT a.id
            FROM Article a
            WHERE a.pipelineStage = :stage
              AND a.pipelineStatus IN :statuses
              AND a.pipelineRetryCount < :maxRetryCount
            ORDER BY a.pipelineUpdatedAt, a.id
            """)
    List<Long> findPipelineCandidateIds(@Param("stage") ArticlePipelineStage stage,
                                        @Param("statuses") List<Status> statuses,
                                        @Param("maxRetryCount") int maxRetryCount,
                                        Pageable pageable);

    @Query("""
            SELECT a.id
            FROM Article a
            WHERE a.pipelineStage = :stage
              AND a.pipelineStatus = :processingStatus
              AND a.pipelineRetryCount < :maxRetryCount
              AND (a.pipelineUpdatedAt IS NULL OR a.pipelineUpdatedAt < :threshold)
            ORDER BY a.pipelineUpdatedAt, a.id
            """)
    List<Long> findStalePipelineCandidateIds(@Param("stage") ArticlePipelineStage stage,
                                             @Param("processingStatus") Status processingStatus,
                                             @Param("maxRetryCount") int maxRetryCount,
                                             @Param("threshold") LocalDateTime threshold,
                                             Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :processingStatus,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :translationStage
              AND a.pipelineStatus = :pendingStatus
            """)
    int markTranslationProcessing(@Param("articleId") Long articleId,
                                  @Param("translationStage") ArticlePipelineStage translationStage,
                                  @Param("pendingStatus") Status pendingStatus,
                                  @Param("processingStatus") Status processingStatus,
                                  @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStage = :dailyChunkStage,
                a.pipelineStatus = :pendingStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :translationStage
              AND a.pipelineStatus IN :translationStatuses
            """)
    int advanceTranslationToDailyChunk(@Param("articleId") Long articleId,
                                       @Param("translationStage") ArticlePipelineStage translationStage,
                                       @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                                       @Param("translationStatuses") List<Status> translationStatuses,
                                       @Param("pendingStatus") Status pendingStatus,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :processingStatus,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :dailyChunkStage
              AND a.pipelineStatus IN :claimableStatuses
              AND a.pipelineRetryCount < :maxRetryCount
            """)
    int claimDailyChunk(@Param("articleId") Long articleId,
                        @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                        @Param("claimableStatuses") List<Status> claimableStatuses,
                        @Param("processingStatus") Status processingStatus,
                        @Param("maxRetryCount") int maxRetryCount,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStage = :completedStage,
                a.pipelineStatus = :successStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :dailyChunkStage
              AND a.pipelineStatus = :processingStatus
            """)
    int completeDailyChunk(@Param("articleId") Long articleId,
                           @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                           @Param("completedStage") ArticlePipelineStage completedStage,
                           @Param("processingStatus") Status processingStatus,
                           @Param("successStatus") Status successStatus,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :failedStatus,
                a.pipelineRetryCount = a.pipelineRetryCount + 1,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :dailyChunkStage
              AND a.pipelineStatus = :processingStatus
              AND a.pipelineRetryCount < :maxRetryCount
            """)
    int failDailyChunk(@Param("articleId") Long articleId,
                       @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                       @Param("processingStatus") Status processingStatus,
                       @Param("failedStatus") Status failedStatus,
                       @Param("maxRetryCount") int maxRetryCount,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :failedStatus,
                a.pipelineRetryCount = a.pipelineRetryCount + 1,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :dailyChunkStage
              AND a.pipelineStatus = :processingStatus
              AND a.pipelineRetryCount < :maxRetryCount
              AND (a.pipelineUpdatedAt IS NULL OR a.pipelineUpdatedAt < :threshold)
            """)
    int recoverStaleDailyChunk(@Param("articleId") Long articleId,
                               @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                               @Param("processingStatus") Status processingStatus,
                               @Param("failedStatus") Status failedStatus,
                               @Param("maxRetryCount") int maxRetryCount,
                               @Param("threshold") LocalDateTime threshold,
                               @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :failedStatus,
                a.pipelineRetryCount = a.pipelineRetryCount + 1,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :translationStage
              AND a.pipelineStatus IN :activeStatuses
            """)
    int failTranslationPipeline(@Param("articleId") Long articleId,
                                @Param("translationStage") ArticlePipelineStage translationStage,
                                @Param("activeStatuses") List<Status> activeStatuses,
                                @Param("failedStatus") Status failedStatus,
                                @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :pendingStatus,
                a.pipelineRetryCount = 0,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :translationStage
              AND a.pipelineStatus = :failedStatus
            """)
    int resetFailedTranslationPipeline(@Param("articleId") Long articleId,
                                       @Param("translationStage") ArticlePipelineStage translationStage,
                                       @Param("failedStatus") Status failedStatus,
                                       @Param("pendingStatus") Status pendingStatus,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :processingStatus,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :sentenceSplitStage
              AND a.pipelineStatus IN :claimableStatuses
              AND a.pipelineRetryCount < :maxRetryCount
            """)
    int claimSentenceSplit(@Param("articleId") Long articleId,
                           @Param("sentenceSplitStage") ArticlePipelineStage sentenceSplitStage,
                           @Param("claimableStatuses") List<Status> claimableStatuses,
                           @Param("processingStatus") Status processingStatus,
                           @Param("maxRetryCount") int maxRetryCount,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :failedStatus,
                a.pipelineRetryCount = a.pipelineRetryCount + 1,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :sentenceSplitStage
              AND a.pipelineStatus = :processingStatus
              AND a.pipelineRetryCount < :maxRetryCount
            """)
    int failSentenceSplit(@Param("articleId") Long articleId,
                          @Param("sentenceSplitStage") ArticlePipelineStage sentenceSplitStage,
                          @Param("processingStatus") Status processingStatus,
                          @Param("failedStatus") Status failedStatus,
                          @Param("maxRetryCount") int maxRetryCount,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Article a
            SET a.pipelineStatus = :failedStatus,
                a.pipelineRetryCount = a.pipelineRetryCount + 1,
                a.pipelineUpdatedAt = :updatedAt
            WHERE a.id = :articleId
              AND a.pipelineStage = :sentenceSplitStage
              AND a.pipelineStatus = :processingStatus
              AND a.pipelineRetryCount < :maxRetryCount
              AND (a.pipelineUpdatedAt IS NULL OR a.pipelineUpdatedAt < :threshold)
            """)
    int recoverStaleSentenceSplit(@Param("articleId") Long articleId,
                                  @Param("sentenceSplitStage") ArticlePipelineStage sentenceSplitStage,
                                  @Param("processingStatus") Status processingStatus,
                                  @Param("failedStatus") Status failedStatus,
                                  @Param("maxRetryCount") int maxRetryCount,
                                  @Param("threshold") LocalDateTime threshold,
                                  @Param("updatedAt") LocalDateTime updatedAt);
}
