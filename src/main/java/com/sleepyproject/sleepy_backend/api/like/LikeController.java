package com.sleepyproject.sleepy_backend.api.like;

import com.sleepyproject.sleepy_backend.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 게시글 또는 댓글 좋아요(추천) 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 특정 게시글/댓글에 대해 좋아요를 등록하거나 해제(토글)합니다. (인증 필요)
     *
     * @param targetId       좋아요 대상 ID (게시글 또는 댓글 ID)
     * @param targetType     좋아요 대상 종류 (예: POST, COMMENT)
     * @param authentication 유저 인증 객체
     * @return 좋아요 상태 (liked: true/false)
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleLike(@RequestParam Long targetId, @RequestParam String targetType, Authentication authentication) {
        boolean isLiked = likeService.toggleLike(targetId, targetType, authentication.getName());
        return ResponseEntity.ok(Map.of("liked", isLiked));
    }
}
