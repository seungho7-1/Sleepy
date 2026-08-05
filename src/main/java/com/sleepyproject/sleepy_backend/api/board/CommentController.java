package com.sleepyproject.sleepy_backend.api.board;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.service.board.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/board")
@RequiredArgsConstructor
@RestController
public class CommentController {
    private final CommentService commentService;


    /**
     * 특정 게시글 또는 상품의 댓글 목록을 조회합니다. (비로그인 허용)
     *
     * @param targetId   댓글 조회 대상 ID (게시글 ID 또는 상품 ID)
     * @param targetType 대상 종류 (예: POST, PRODUCT)
     * @return 댓글 목록 리스트
     */
    @GetMapping("/comments")
    public ResponseEntity<?> getComments(@RequestParam Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(commentService.getComments(targetId, targetType));
    }


    /**
     * 특정 게시글/상품에 댓글을 등록합니다. (인증 필요)
     *
     * @param request        댓글 작성 정보 DTO
     * @param authentication 유저 인증 객체
     * @return 생성된 댓글 ID
     */
    @PostMapping("/comments")
    public ResponseEntity<?> createComment(@RequestBody CommentRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "Unknown";
        try {
            Long commentId = commentService.createComment(request, username);
            return ResponseEntity.ok(commentId);
        } catch (Exception e) {
            throw e;
        }
    }


    /**
     * 내가 작성한 댓글 목록을 조회합니다. (인증 필요)
     *
     * @return 내가 쓴 댓글 목록 페이지
     */
    @GetMapping("/my-comments")
    public ResponseEntity<?> getMyComments(
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(commentService.getMyComments(authentication.getName(), pageable));
    }

    /**
     * 내가 쓴 댓글의 내용을 수정합니다. (인증 필요)
     *
     * @param id             수정할 댓글 ID
     * @param request        수정할 댓글 내용 DTO
     * @param authentication 유저 인증 객체
     * @return HTTP 200 OK
     */
    @PutMapping("/comments/{id}")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody CommentRequest request, Authentication authentication) {
        commentService.updateComment(id, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * 내가 쓴 댓글을 삭제합니다. (인증 필요)
     *
     * @param id             삭제할 댓글 ID
     * @param authentication 유저 인증 객체
     * @return HTTP 200 OK
     */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication authentication) {
        commentService.deleteComment(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
