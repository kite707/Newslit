package com.newslit.backend.readingHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Optional<List<ReadingHistory>> findAllByUserIdAndDisplayDateBetween(Long id, LocalDate startDate,
                                                                        LocalDate endDate);
}
