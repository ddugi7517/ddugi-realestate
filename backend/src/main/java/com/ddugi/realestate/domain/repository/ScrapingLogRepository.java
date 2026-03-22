package com.ddugi.realestate.domain.repository;

import com.ddugi.realestate.domain.entity.ScrapingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapingLogRepository extends JpaRepository<ScrapingLog, Long> {

    List<ScrapingLog> findTop10ByOrderByStartedAtDesc();

    List<ScrapingLog> findBySourceOrderByStartedAtDesc(ScrapingLog.ScrapingSource source);
}
