package com.sleepyproject.sleepy_backend.domain.review;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리뷰 신고 정보를 저장하는 엔티티입니다.
 * 중복 신고를 방지하기 위해 리뷰와 회원을 연결합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "review_report", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "member_id"})
})
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private LocalDateTime createdAt;

    @Builder
    public ReviewReport(Review review, Member member, LocalDateTime createdAt) {
        this.review = review;
        this.member = member;
        this.createdAt = createdAt;
    }
}
