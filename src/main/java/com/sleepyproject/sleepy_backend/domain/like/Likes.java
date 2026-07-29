package com.sleepyproject.sleepy_backend.domain.like;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 또는 댓글/리뷰에 등록된 좋아요(추천) 기록을 저장하는 도메인 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "targetId", "targetType"})
})
public class Likes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Long targetId; // 게시글의 ID 나중에 확장 가능성 (Comment,Product)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Builder
    public Likes(Member member, Long targetId, TargetType targetType) {
        this.member = member;
        this.targetId = targetId;
        this.targetType = targetType;
    }

}
