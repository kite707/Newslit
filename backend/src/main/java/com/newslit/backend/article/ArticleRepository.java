package com.newslit.backend.article;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

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
}
