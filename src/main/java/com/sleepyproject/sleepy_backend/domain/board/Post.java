package com.sleepyproject.sleepy_backend.domain.board;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 커뮤니티 게시글 정보를 저장하는 도메인 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post", indexes = {
    @Index(name = "idx_post_board_type", columnList = "boardType"),
    @Index(name = "idx_post_created_at", columnList = "createdAt"),
    @Index(name = "idx_post_popularity_score", columnList = "popularityScore")
})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType boardType;

    private String imageUrl; // 사진/영상 경로

    private int viewCount = 0;
    private int likeCount = 0;
    private int commentCount = 0;

    @Column(nullable = false)
    private Double popularityScore = 0.0;

    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isHidden = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_hashtags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "hashtag")
    private List<String> hashtags = new ArrayList<>();

    @Builder
    public Post(Member member, String title, String content, BoardType boardType, String imageUrl, LocalDateTime createdAt, List<String> hashtags) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.boardType = boardType;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        if (hashtags != null) {
            this.hashtags.addAll(hashtags);
        }
    }

    /**
     * 게시글 조회수를 1 증가시킵니다.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
        this.calculatePopularityScore();
    }

    /**
     * 게시글 좋아요 수를 1 감소시킵니다.
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
            this.calculatePopularityScore();
        }
    }

    /**
     * 댓글 수를 1 증가시킵니다.
     */
    public void incrementCommentCount() {
        this.commentCount++;
        this.calculatePopularityScore();
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
            this.calculatePopularityScore();
        }
    }

    /**
     * 조회수, 좋아요 수, 댓글 수 및 작성 후 경과 시간을 반영하여 인기 점수를 재계산합니다.
     */
    public void calculatePopularityScore() {
        if (this.createdAt == null) {
            this.popularityScore = 0.0;
            return;
        }
        double baseScore = (this.likeCount * 3) + (this.commentCount * 2) + (this.viewCount * 0.1);
        long hoursPassed = java.time.Duration.between(this.createdAt, LocalDateTime.now()).toHours();
        // 0 미만 방지 및 시간 감쇠 수식 (Hacker News 스타일 변형)
        this.popularityScore = baseScore / Math.pow(Math.max(hoursPassed, 0) + 2, 1.5);
    }

    public void update(String title, String content, String imageUrl, java.util.List<String> tags) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        if (tags != null) {
            this.hashtags.clear();
            this.hashtags.addAll(tags);
        }
    }

    public void hide() {
        this.isHidden = true;
    }

    public void unhide() {
        this.isHidden = false;
    }
}
