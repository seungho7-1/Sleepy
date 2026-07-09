package com.sleepyproject.sleepy_backend.api.like;

import com.sleepyproject.sleepy_backend.service.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleLike(@RequestParam Long targetId, @RequestParam String targetType, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean isLiked = likeService.toggleLike(targetId, targetType, authentication.getName());
        return ResponseEntity.ok(Map.of("liked", isLiked));
    }
}
