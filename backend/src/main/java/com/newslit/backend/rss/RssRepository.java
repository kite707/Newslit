package com.newslit.backend.rss;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RssRepository extends JpaRepository<Rss, Long> {
    Optional<Rss> findById(Long id);
}
