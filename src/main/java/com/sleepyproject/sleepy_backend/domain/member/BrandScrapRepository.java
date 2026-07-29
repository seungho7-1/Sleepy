package com.sleepyproject.sleepy_backend.domain.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandScrapRepository extends JpaRepository<BrandScrap, Long> {

    Optional<BrandScrap> findByMemberIdAndSellerId(Long memberId, Long sellerId);

    long countBySellerId(Long sellerId);

    boolean existsByMemberIdAndSellerId(Long memberId, Long sellerId);

    Page<BrandScrap> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
