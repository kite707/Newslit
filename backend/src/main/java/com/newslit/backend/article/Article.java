package com.newslit.backend.article;

import com.newslit.backend.daily.Daily;
import com.newslit.backend.global.common.enums.Status;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.vocabulary.Vocabulary;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(
        name = "articles",
        indexes = {
                @Index(
                        name = "idx_article_pipeline_work",
                        columnList = "pipeline_stage, pipeline_status, pipeline_updated_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_article_title_source",
                        columnNames = {"title", "source"}
                )
        }
)
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long Id;

    @Column(length = 500, nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(nullable = false)
    private String originalText;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(name = "audio_download_link", length = 500)
    private String audioDownloadLink;

    @Column(name = "audio_link", length = 500)
    private String audioLink;

    @Column(nullable = false)
    private LocalDate publishedDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "source")
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_stage", nullable = false)
    @ColumnDefault("'SENTENCE_SPLIT'")
    @Builder.Default
    private ArticlePipelineStage pipelineStage = ArticlePipelineStage.SENTENCE_SPLIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status", nullable = false)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private Status pipelineStatus = Status.PENDING;

    @Column(name = "pipeline_retry_count", nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private int pipelineRetryCount = 0;

    @CreationTimestamp
    @Column(name = "pipeline_updated_at", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime pipelineUpdatedAt;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Sentence> sentences = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Vocabulary> vocabularies = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Daily> dailies = new ArrayList<>();

}
