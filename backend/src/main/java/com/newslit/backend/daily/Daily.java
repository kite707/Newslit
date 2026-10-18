package com.newslit.backend.daily;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.newslit.backend.article.Article;
import com.newslit.backend.readingHistory.ReadingHistory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "daily_article",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_article_range",
                        columnNames = {"article_id", "start_index", "end_index"}
                ),
                // 하루에 한 건이라는 도메인 규칙을 DB로 강제한다.
                // 여러 인스턴스가 서로 다른 기사를 동시에 청킹하면 displayDate가 겹칠 수 있는데,
                // 이 제약이 있으면 트랜잭션이 롤백되고 다음 주기에 비어 있는 날짜로 다시 생성된다.
                @UniqueConstraint(
                        name = "uk_daily_display_date",
                        columnNames = {"display_date"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Daily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_date", nullable = false)
    private LocalDate displayDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    @JsonIgnore
    private Article article;

    @Column(name = "start_index", nullable = false)
    private Integer startIndex;

    @Column(name = "end_index", nullable = false)
    private Integer endIndex;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "daily", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ReadingHistory> readingHistories = new ArrayList<>();
}
