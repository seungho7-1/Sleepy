package com.sleepyproject.sleepy_backend.api.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequest {
    private Long productId;
    private int rating;
    private String content;
    private String imageUrl;
}
