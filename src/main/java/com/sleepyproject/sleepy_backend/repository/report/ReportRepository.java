package com.sleepyproject.sleepy_backend.repository.report;

import com.sleepyproject.sleepy_backend.domain.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sleepyproject.sleepy_backend.domain.report.ReportStatus;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByStatus(ReportStatus status);
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
