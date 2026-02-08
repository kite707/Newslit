package com.newslit.backend.article;

import com.newslit.backend.daily.Daily;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.vocabulary.Vocabulary;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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

    @Lob
    @Column(nullable = false)
    private String originalText;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 500)
    private String mp3Link;

    @Column(nullable = false)
    private LocalDate publishedDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "source")
    private String source;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Sentence> sentences = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Vocabulary> vocabularies = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Daily> dailies = new ArrayList<>();

}
