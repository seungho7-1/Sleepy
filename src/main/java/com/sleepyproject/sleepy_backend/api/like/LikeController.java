package com.sleepyproject.sleepy_backend.api.like;

import com.sleepyproject.sleepy_backend.api.like.dto.LikeRequestDto;
import com.sleepyproject.sleepy_backend.api.like.dto.LikeResponseDto;
import com.sleepyproject.sleepy_backend.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * @param requestDto       좋아요를 토글할 대상의 ID (게시글 또는 댓글), 타입 (POST 또는 COMMENT)
     * @param authentication 유저 인증 객체
     * @return 좋아요 상태 (liked: true/false)
     */
    @PostMapping("/toggle")
    public ResponseEntity<LikeResponseDto> toggleLike(@RequestBody LikeRequestDto requestDto, Authentication authentication) {
        //boolean isLiked = likeService.toggleLike(requestDto.getTargetId(),requestDto.getTargetType(), authentication.getName());
        LikeResponseDto responseDto = likeService.toggleLike(requestDto, authentication.getName());
        //return ResponseEntity.ok(Map.of("liked", isLiked));
        if (responseDto.isLiked()) {
            // 좋아요가 새로 생성(등록)된 경우: 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } else {
            // 좋아요가 취소(삭제)되었거나 토글된 경우: 200 OK (또는 204 No Content 등)
            return ResponseEntity.ok(responseDto);
        }
    }


}
