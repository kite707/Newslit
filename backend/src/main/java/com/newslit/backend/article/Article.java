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
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(
        name = "articles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_article_title_url",
                        columnNames = {"title", "sourceUrl"}
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

    @Lob
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

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Sentence> sentences = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Vocabulary> vocabularies = new ArrayList<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL)
    private List<Daily> dailies = new ArrayList<>();

}
