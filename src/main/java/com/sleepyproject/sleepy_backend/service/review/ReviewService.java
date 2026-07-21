package com.sleepyproject.sleepy_backend.service.review;

import com.sleepyproject.sleepy_backend.api.review.dto.ReviewRequest;
import com.sleepyproject.sleepy_backend.api.review.dto.ReviewResponse;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 상품 리뷰 생성 및 페이징 조회를 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    /**
     * 리뷰 등록 로직
     * 리뷰 저장 후 해당 상품의 판매자에게 NEW_REVIEW 알림을 발송합니다.
     *
     * @param request 리뷰 생성 요청 DTO (상품 ID, 평점, 내용, 이미지 URL)
     * @param username 요청한 유저 username
     * @return 등록된 리뷰의 ID
     */
    @Transactional
    public Long create(ReviewRequest request, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Review review = Review.builder()
                .product(product)
                .member(member)
                .rating(request.getRating())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        // [판매자 알림] 내 상품에 새 리뷰가 등록되면 판매자에게 알림
        Member seller = product.getSeller();
        if (seller != null && !seller.getId().equals(member.getId())) {
            notificationService.createNotificationByMember(
                    seller,
                    NotificationType.NEW_REVIEW,
                    member.getNickname() + "님이 " + product.getName() + "에 리뷰를 남겼습니다.",
                    "/product/" + product.getId()
            );
        }

        return saved.getId();
    }

    /**
     * 특정 상품의 리뷰 목록 페이징 조회 로직
     *
     * @param productId 상품 ID
     * @param pageable  페이징 정보
     * @return 해당 상품의 리뷰 목록 (ReviewResponse 형태)
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdWithMember(productId, pageable)
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getRating(),
                        r.getContent(),
                        r.getMember().getNickname(),
                        r.getImageUrl(),
                        r.getLikeCount(),
                        r.getCreatedAt()
                ));
    }
}
