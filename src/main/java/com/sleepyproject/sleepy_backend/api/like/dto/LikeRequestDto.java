package com.sleepyproject.sleepy_backend.api.like.dto;

import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequestDto {
    private Long targetId;
    private TargetType targetType;
}
