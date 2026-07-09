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

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@RequestBody PostRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Long postId = boardService.createPost(request, authentication.getName());
        return ResponseEntity.ok(postId);
    }

    @GetMapping("/posts")
    public ResponseEntity<?> getPosts(@RequestParam String type, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(boardService.getPosts(type, pageable));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getPostDetail(id));
    }

    @PostMapping("/comments")
    public ResponseEntity<?> createComment(@RequestBody CommentRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Long commentId = boardService.createComment(request, authentication.getName());
        return ResponseEntity.ok(commentId);
    }

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(@RequestParam Long targetId, @RequestParam String targetType) {
        return ResponseEntity.ok(boardService.getComments(targetId, targetType));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPosts(@RequestParam(required = false) String type, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(boardService.getMyPosts(authentication.getName(), type));
    }

    @GetMapping("/my-comments")
    public ResponseEntity<?> getMyComments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(boardService.getMyComments(authentication.getName()));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody CommentRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boardService.updateComment(id, request, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boardService.deleteComment(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
