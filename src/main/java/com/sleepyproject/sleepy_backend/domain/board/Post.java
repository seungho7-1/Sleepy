package com.sleepyproject.sleepy_backend.domain.board;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
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

    @Builder
    public Post(Member member, String title, String content, BoardType boardType, String imageUrl, LocalDateTime createdAt) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.boardType = boardType;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}
