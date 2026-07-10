package com.sleepyproject.sleepy_backend.domain.board;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 또는 리뷰에 작성되는 댓글 정보를 저장하는 도메인 엔티티 클래스입니다.
 * - 대댓글(대댓글 관계) 처리를 위한 셀프 조인(parent, children) 연관관계를 포함합니다.
 */
@Entity
@Getter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime createdAt;

    /**
     * 댓글 내용을 수정합니다.
     *
     * @param content 수정할 새 댓글 내용
     */
    public void updateContent(String content) {
        this.content = content;
    }

    @Builder
    public Comment(Member member, Post post, Review review, Comment parent, String content, LocalDateTime createdAt) {
        this.member = member;
        this.post = post;
        this.review = review;
        this.parent = parent;
        this.content = content;
        this.createdAt = createdAt;
    }
}
