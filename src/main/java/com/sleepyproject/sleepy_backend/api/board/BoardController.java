package com.sleepyproject.sleepy_backend.api.board;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.PostRequest;
import com.sleepyproject.sleepy_backend.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 커뮤니티 게시글 및 댓글 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /**
     * 새로운 커뮤니티 게시글을 생성합니다. (인증 필요)
     *
     * @param request        게시글 등록 데이터 DTO
     * @param authentication 유저 인증 객체
     * @return 생성된 게시글 ID
     */
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody PostRequest request, Authentication authentication) {
        Long postId = boardService.createPost(request, authentication.getName());
        return ResponseEntity.ok(postId);
    }

    /**
     * 특정 종류의 게시글 목록을 조회합니다. (페이징 지원, 비로그인 허용)
     *
     * @param type     게시판 종류 (예: FREE, QNA, NOTICE, MEDIA 등)
     * @param pageable 페이징 정보 (Spring Data Pageable)
     * @return 페이징 처리된 게시글 목록
     */
    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(
            @RequestParam String type, 
            @RequestParam(required = false, defaultValue = "") String keyword, 
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(boardService.getPosts(type, keyword, pageable, username));
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다. (비로그인 허용)
     *
     * @param id 조회할 게시글 ID
     * @return 게시글 상세 내용 DTO
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(boardService.getPostDetail(id, username));
    }

    @PostMapping("/posts/{id}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        String username = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
        String identifier = username != null ? username : request.getRemoteAddr();
        return ResponseEntity.ok(boardService.incrementViewCount(id, identifier, username));
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(boardService.toggleLike(id, authentication.getName()));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody PostRequest request, Authentication authentication) {
        boardService.updatePost(id, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication authentication) {
        boardService.deletePost(id, authentication.getName());
        return ResponseEntity.ok().build();
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
        Long commentId = boardService.createComment(request, authentication.getName());
        return ResponseEntity.ok(commentId);
    }

    /**
     * 특정 게시글 또는 상품의 댓글 목록을 조회합니다. (비로그인 허용)
     *
     * @param targetId   댓글 조회 대상 ID (게시글 ID 또는 상품 ID)
     * @param targetType 대상 종류 (예: POST, PRODUCT)
     * @return 댓글 목록 리스트
     */
    @GetMapping("/comments")
    public ResponseEntity<?> getComments(@RequestParam Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(boardService.getComments(targetId, targetType));
    }

    /**
     * 내가 작성한 게시글 목록을 조회합니다. (인증 필요)
     *
     * @param type           필터링할 게시판 종류 (선택)
     * @param authentication 유저 인증 객체
     * @return 내가 쓴 게시글 목록 리스트
     */
    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPosts(@RequestParam(required = false) String type, Authentication authentication) {
        return ResponseEntity.ok(boardService.getMyPosts(authentication.getName(), type));
    }

    /**
     * 내가 작성한 댓글 목록을 조회합니다. (인증 필요)
     *
     * @param authentication 유저 인증 객체
     * @return 내가 쓴 댓글 목록 리스트
     */
    @GetMapping("/my-comments")
    public ResponseEntity<?> getMyComments(Authentication authentication) {
        return ResponseEntity.ok(boardService.getMyComments(authentication.getName()));
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
        boardService.updateComment(id, request, authentication.getName());
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
        boardService.deleteComment(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
