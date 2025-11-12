package com.newslit.backend.article;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findById(Long id);

    Optional<Article> findArticleByDisplayDate(LocalDate date);

    Optional<Article> findByTitleAndSource(String title, String source);
}
