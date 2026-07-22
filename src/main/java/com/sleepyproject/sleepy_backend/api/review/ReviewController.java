package com.sleepyproject.sleepy_backend.api.review;

import com.sleepyproject.sleepy_backend.api.review.dto.ReviewRequest;
import com.sleepyproject.sleepy_backend.api.review.dto.ReviewResponse;
import com.sleepyproject.sleepy_backend.api.review.dto.SellerReviewResponse;
import com.sleepyproject.sleepy_backend.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 리뷰 관련 HTTP 요청을 수신 및 처리하는 컨트롤러 클래스입니다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 특정 상품에 대해 등록된 리뷰 목록을 페이징하여 조회합니다. (비로그인 허용)
     *
     * @param productId 상품 ID
     * @param pageable  페이징 정보 (기본값: 최신순 20개)
     * @return 페이징 처리된 리뷰 목록 DTO
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable("productId") Long productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<ReviewResponse> responses = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 상품에 대한 리뷰를 작성합니다. (인증 필요)
     *
     * @param request        작성할 리뷰 정보 DTO
     * @param authentication 현재 로그인된 유저 인증 정보
     * @return 생성된 리뷰 ID
     */
    @PostMapping
    public ResponseEntity<Long> createReview(
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        
        Long reviewId = reviewService.create(request, authentication.getName());
        return ResponseEntity.ok(reviewId);
    }

    /**
     * 리뷰 신고 API
     */
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<Void> reportReview(
            @PathVariable("reviewId") Long reviewId,
            Authentication authentication) {
        reviewService.reportReview(reviewId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 판매자의 모든 리뷰 조회 (판매자 대시보드용)
     */
    @GetMapping("/seller")
    public ResponseEntity<List<SellerReviewResponse>> getReviewsBySeller(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getReviewsBySeller(authentication.getName()));
    }
}
