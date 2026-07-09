package com.sleepyproject.sleepy_backend.api.review;

import com.sleepyproject.sleepy_backend.api.review.dto.ReviewRequest;
import com.sleepyproject.sleepy_backend.api.review.dto.ReviewResponse;
import com.sleepyproject.sleepy_backend.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 특정 상품의 리뷰 목록 조회 (페이징, 비로그인 유저도 볼 수 있음)
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable("productId") Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<ReviewResponse> responses = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(responses);
    }

    // 리뷰 작성 (로그인한 유저만 작성 가능)
    @PostMapping
    public ResponseEntity<Long> createReview(
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        
        Long reviewId = reviewService.create(request, authentication.getName());
        return ResponseEntity.ok(reviewId);
    }
}
