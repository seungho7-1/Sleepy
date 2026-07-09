package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String boardType;
    private String imageUrl;
    private String nickname;
    private int viewCount;
    private int likeCount;
    private LocalDateTime createdAt;
}
