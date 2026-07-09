package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequest {
    private Long targetId; // Post ID or Review ID
    private String targetType; // "POST" or "REVIEW"
    private String content;
}
