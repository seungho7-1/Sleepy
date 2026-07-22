package com.sleepyproject.sleepy_backend.api.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SellerReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private int rating;
    private String content;
    private String nickname;
    private String imageUrl;
    private boolean isHidden;
    private int reportCount;
    private LocalDateTime createdAt;
}
