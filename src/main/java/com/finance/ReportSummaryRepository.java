package com.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportSummaryRepository extends JpaRepository<ReportSummary, Long> {

    Optional<ReportSummary> findTopByOrderByGeneratedAtDesc();

    List<ReportSummary> findTop10ByOrderByGeneratedAtDesc();
}
