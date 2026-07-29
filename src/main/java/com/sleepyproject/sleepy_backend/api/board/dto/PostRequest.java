package com.sleepyproject.sleepy_backend.api.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostRequest {
    private String title;
    private String content;
    private String boardType; // FREE, QNA, NOTICE, MEDIA
    private String imageUrl;
    private List<String> hashtags;
}
