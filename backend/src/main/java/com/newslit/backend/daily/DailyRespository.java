package com.newslit.backend.daily;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRespository extends JpaRepository<Daily, Long> {
    Daily findByDisplayDate(LocalDate date);

    List<Daily> findAllByDisplayDateBetween(LocalDate start, LocalDate end);

    List<Daily> findByArticleIdOrderByIdAsc(Long articleId);

    Optional<Daily> findTopByOrderByDisplayDateDesc();

    Optional<Daily> findByArticleIdAndStartIndexAndEndIndex(Long articleId, int startIndex, int endIndex);
}
