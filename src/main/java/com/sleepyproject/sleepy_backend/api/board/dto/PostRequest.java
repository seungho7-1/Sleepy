package com.sleepyproject.sleepy_backend.api.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "게시판 종류는 필수입니다.")
    private String boardType; // FREE, QNA, NOTICE, MEDIA

    private String imageUrl;
    private String thumbnailUrl;
    private List<String> hashtags; // 해시태그 목록 추가
    private Boolean isPinned; // 상단 고정 여부 (관리자용)
}
