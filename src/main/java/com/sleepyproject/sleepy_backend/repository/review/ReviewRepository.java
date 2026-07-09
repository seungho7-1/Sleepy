package com.sleepyproject.sleepy_backend.repository.review;

import com.sleepyproject.sleepy_backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // N+1 문제 방지를 위해 fetch join 사용 (리뷰 조회 시 작성자 닉네임을 함께 가져옴)
    @Query(value = "SELECT r FROM Review r JOIN FETCH r.member WHERE r.product.id = :productId",
           countQuery = "SELECT count(r) FROM Review r WHERE r.product.id = :productId")
    Page<Review> findByProductIdWithMember(@Param("productId") Long productId, Pageable pageable);
}
