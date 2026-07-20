package com.sleepyproject.sleepy_backend.domain.board;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커뮤니티 게시글 정보를 저장하는 도메인 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post", indexes = {
    @Index(name = "idx_post_board_type", columnList = "boardType"),
    @Index(name = "idx_post_created_at", columnList = "createdAt")
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

    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isHidden = false;

    @Builder
    public Post(Member member, String title, String content, BoardType boardType, String imageUrl, LocalDateTime createdAt) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.boardType = boardType;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    /**
     * 게시글 조회수를 1 증가시킵니다.
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 게시글 좋아요 수를 1 증가시킵니다.
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 게시글 좋아요 수를 1 감소시킵니다.
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void update(String title, String content, String imageUrl, java.util.List<String> tags) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void hide() {
        this.isHidden = true;
    }

    public void unhide() {
        this.isHidden = false;
    }
}
