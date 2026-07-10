package com.sleepyproject.sleepy_backend.domain.review;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품에 대해 작성된 리뷰 정보를 저장하는 도메인 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int rating; // 별점 (1~5)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    private String imageUrl; // 리뷰 사진
    
    private int likeCount = 0; // 좋아요 수

    private LocalDateTime createdAt;

    @Builder
    public Review(Product product, Member member, int rating, String content, String imageUrl, LocalDateTime createdAt) {
        this.product = product;
        this.member = member;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
        this.likeCount = 0;
        this.createdAt = createdAt;
    }
    
    /**
     * 리뷰 좋아요 수를 1 증가시킵니다.
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }
    
    /**
     * 리뷰 좋아요 수를 1 감소시킵니다.
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
