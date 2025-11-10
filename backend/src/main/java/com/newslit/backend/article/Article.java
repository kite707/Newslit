package com.newslit.backend.article;

import com.newslit.backend.common.enums.Status;
import com.newslit.backend.vocabulary.Vocabulary;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

@Entity
@Table(name = "articles")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long Id;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String translatedText;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    private LocalDate publishedDate;

    @Column(unique = true)
    private LocalDate displayDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "source")
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    private Status translationStatus = Status.PENDING;

    //    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
//    private List<Sentence> sentences = new ArrayList<>();
//
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Vocabulary> vocabularies = new ArrayList<>();
}
