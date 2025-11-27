package com.newslit.backend.daily;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyRespository extends JpaRepository<Daily, Long> {
    Daily findByDisplayDate(LocalDate date);

    List<Daily> findAllByDisplayDateBetween(LocalDate start, LocalDate end);
}
