package com.sleepyproject.sleepy_backend.api.board;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.PostRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.PostResponse;
import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 커뮤니티 게시글 및 댓글 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 */
@Slf4j
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
    public ResponseEntity<Long> createPost(@Valid @RequestBody PostRequest request, Authentication authentication) {
        Long postId = boardService.createPost(request, authentication.getName());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(postId);
    }
    /**
     * 특정 종류의 게시글 목록을 조회합니다. (페이징 지원, 비로그인 허용)
     *
     * @param type     게시판 종류 (예: FREE, QNA, NOTICE, MEDIA 등)
     * @param pageable 페이징 정보 (Spring Data Pageable)
     * @return 페이징 처리된 게시글 목록
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam("type") String type,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        org.springframework.data.domain.Sort newSort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "isPinned").and(pageable.getSort());
        org.springframework.data.domain.Pageable newPageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
        return ResponseEntity.ok(boardService.getPosts(type, keyword, newPageable, username));
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다. (비로그인 허용)
     *
     * @param id 조회할 게시글 ID
     * @return 게시글 상세 내용 DTO
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getPostDetail(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        PostResponse postResponse = boardService.getPostDetail(id, username);
        return ResponseEntity.ok(postResponse);
    }

    @PostMapping("/posts/{id}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        String username = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
        String identifier = username != null ? username : request.getRemoteAddr();
        return ResponseEntity.ok(boardService.incrementViewCount(id, identifier, username));
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
     * 내가 작성한 게시글 목록을 조회합니다. (인증 필요)
     *
     * @param type           필터링할 게시판 종류 (선택)
     * @param authentication 유저 인증 객체
     * @return 내가 쓴 게시글 목록 리스트
     */
    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPosts(
            @RequestParam(required = false) String type,
            @org.springframework.data.web.PageableDefault(size = 10) org.springframework.data.domain.Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(boardService.getMyPosts(authentication.getName(), type, pageable));
    }
}
