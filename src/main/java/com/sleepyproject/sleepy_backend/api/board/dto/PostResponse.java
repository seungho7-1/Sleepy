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
    private String authorRole; // 작성자 역할 (ROLE_USER, ROLE_ADMIN 등)
    private int viewCount;
    private int likeCount;
    private LocalDateTime createdAt;
    private String profileImageUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked;
    @com.fasterxml.jackson.annotation.JsonProperty("isCommented")
    private boolean isCommented;
    private int commentCount;
    private double popularityScore;
    @com.fasterxml.jackson.annotation.JsonProperty("isPinned")
    private boolean isPinned;
    private List<String> hashtags;

    public boolean getIsPinned() {
        return isPinned;
    }
}
