package com.newslit.backend.daily;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newslit.backend.article.Article;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "daily_article")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Daily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_date")
    private LocalDate displayDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    @JsonIgnore
    private Article article;

    @Column(name = "start_index")
    private Integer startIndex;

    @Column(name = "end_index")
    private Integer endIndex;

    @Column(name = "word_count")
    private Integer wordCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
