package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostRequest {
    private String title;
    private String content;
    private String boardType; // FREE, QNA, NOTICE, MEDIA
    private String imageUrl;
}
