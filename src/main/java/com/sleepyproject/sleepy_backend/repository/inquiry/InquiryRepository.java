package com.sleepyproject.sleepy_backend.repository.inquiry;

import com.sleepyproject.sleepy_backend.domain.inquiry.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<Inquiry> findAllByOrderByCreatedAtDesc();
}
