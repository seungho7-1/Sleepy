package com.sleepyproject.sleepy_backend.repository.review;

import com.sleepyproject.sleepy_backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // N+1 문제 방지를 위해 fetch join 사용 (리뷰 조회 시 작성자 닉네임을 함께 가져옴)
    @Query(value = "SELECT r FROM Review r JOIN FETCH r.member WHERE r.product.id = :productId AND r.isHidden = false",
           countQuery = "SELECT count(r) FROM Review r WHERE r.product.id = :productId AND r.isHidden = false")
    Page<Review> findByProductIdWithMember(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN FETCH r.member JOIN FETCH r.product p WHERE p.seller.id = :sellerId ORDER BY r.createdAt DESC")
    java.util.List<Review> findByProductSellerId(@Param("sellerId") Long sellerId);

    void deleteByProductId(Long productId);
    
    long countByProductId(Long productId);

    @Query("SELECT COUNT(r), COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId AND r.isHidden = false")
    java.util.List<Object[]> getReviewStatsByProductId(@Param("productId") Long productId);

    /**
     * 리뷰 좋아요 수 원자적 증가 (+1) - 동시성 안전
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId AND r.likeCount >= 0")
    void incrementLikeCount(@Param("reviewId") Long reviewId);

    /**
     * 리뷰 좋아요 수 원자적 감소 (-1, 0 미만 방지) - 동시성 안전
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Review r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId AND r.likeCount > 0")
    void decrementLikeCount(@Param("reviewId") Long reviewId);
}
