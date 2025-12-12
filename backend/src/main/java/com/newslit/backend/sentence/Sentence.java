package com.newslit.backend.sentence;

import com.newslit.backend.article.Article;
import com.newslit.backend.global.common.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sentences", indexes = {
        @Index(name = "sentences_index_3", columnList = "article_id, order_index")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false, foreignKey = @ForeignKey(name = "sentences_ibfk_1"))
    private Article article;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Lob
    @Column(name = "english_text", nullable = false)
    private String englishText;

    @Lob
    @Column(name = "korean_text")
    private String koreanText;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private Status translationStatus = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
