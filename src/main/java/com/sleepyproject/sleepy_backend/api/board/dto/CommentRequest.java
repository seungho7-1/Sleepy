package com.sleepyproject.sleepy_backend.api.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequest {
    private Long targetId; // Post ID or Review ID
    private String targetType; // "POST" or "REVIEW"
    @NotBlank(message = "댓글은 필수입니다.")
    private String content;
    private Long parentId; // 부모 댓글 ID (대댓글용, 선택사항)
}
