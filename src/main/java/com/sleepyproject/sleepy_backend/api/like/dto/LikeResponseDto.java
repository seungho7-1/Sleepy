package com.sleepyproject.sleepy_backend.api.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class LikeResponseDto {
    private boolean isLiked;
    private int likeCount;
}
