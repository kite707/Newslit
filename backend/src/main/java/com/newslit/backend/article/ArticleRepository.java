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
            WHERE a.pipelineStage = :stage
              AND a.pipelineStatus = :status
            ORDER BY a.pipelineUpdatedAt, a.id
            """)
    List<Long> findPipelineCandidateIds(@Param("stage") ArticlePipelineStage stage,
                                        @Param("status") Status status,
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
              AND a.pipelineStatus = :pendingStatus
            """)
    int claimDailyChunk(@Param("articleId") Long articleId,
                        @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                        @Param("pendingStatus") Status pendingStatus,
                        @Param("processingStatus") Status processingStatus,
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
            """)
    int failDailyChunk(@Param("articleId") Long articleId,
                       @Param("dailyChunkStage") ArticlePipelineStage dailyChunkStage,
                       @Param("processingStatus") Status processingStatus,
                       @Param("failedStatus") Status failedStatus,
                       @Param("updatedAt") LocalDateTime updatedAt);
}
