package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String nickname;
    private LocalDateTime createdAt;
}
