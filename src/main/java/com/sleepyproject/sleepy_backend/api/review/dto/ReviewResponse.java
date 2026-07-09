package com.sleepyproject.sleepy_backend.api.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private int rating;
    private String content;
    private String nickname; // 작성자 닉네임
    private String imageUrl;
    private int likeCount;
    private LocalDateTime createdAt;
}
