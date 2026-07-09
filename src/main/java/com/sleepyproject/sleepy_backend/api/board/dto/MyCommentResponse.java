package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyCommentResponse {
    private Long id;
    private String content;
    private String nickname;
    private LocalDateTime createdAt;
    private Long targetId;
    private String targetType;  // "POST" or "REVIEW"
    private String targetTitle; // 원본 글 제목 또는 리뷰 대상 상품명
}
