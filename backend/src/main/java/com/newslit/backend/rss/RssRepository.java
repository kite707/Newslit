package com.newslit.backend.rss;

import com.newslit.backend.global.common.enums.Status;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RssRepository extends JpaRepository<Rss, Long> {
    Optional<Rss> findById(Long id);

    boolean existsByUrl(String url);

    Optional<Rss> findFirstByStatusOrderByIdAsc(Status status);
}
