package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String boardType;
    private String imageUrl;
    private String thumbnailUrl;
    private String nickname;
    private int viewCount;
    private int likeCount;
    private LocalDateTime createdAt;
    private String profileImageUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked; // 현재 유저의 좋아요 여부
    @com.fasterxml.jackson.annotation.JsonProperty("isCommented")
    private boolean isCommented; // 현재 유저의 댓글 여부
    private int commentCount; // 댓글 수
    private double popularityScore; // 인기 점수
    @com.fasterxml.jackson.annotation.JsonProperty("isPinned")
    private boolean isPinned; // 상단 고정 여부
    private List<String> hashtags;

    public boolean getIsPinned() {
        return isPinned;
    }
}
