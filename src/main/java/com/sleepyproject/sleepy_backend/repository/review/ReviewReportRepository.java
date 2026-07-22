package com.sleepyproject.sleepy_backend.repository.review;

import com.sleepyproject.sleepy_backend.domain.review.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    Optional<ReviewReport> findByReviewIdAndMemberId(Long reviewId, Long memberId);
}
