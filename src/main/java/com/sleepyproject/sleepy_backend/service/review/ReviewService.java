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
import com.sleepyproject.sleepy_backend.repository.review.ReviewReportRepository;
import com.sleepyproject.sleepy_backend.repository.report.ReportRepository;
import com.sleepyproject.sleepy_backend.domain.review.ReviewReport;
import com.sleepyproject.sleepy_backend.domain.report.Report;
import com.sleepyproject.sleepy_backend.domain.report.ReportTargetType;
import com.sleepyproject.sleepy_backend.domain.report.ReportStatus;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sleepyproject.sleepy_backend.api.review.dto.SellerReviewResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 리뷰 생성 및 페이징 조회를 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final com.sleepyproject.sleepy_backend.util.BadWordFilter badWordFilter;

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

        // 금칙어 필터링
        String content = request.getContent();
        if (content != null) {
            content = badWordFilter.filter(content);
        }

        Review review = Review.builder()
                .product(product)
                .member(member)
                .rating(request.getRating())
                .content(content)
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
                        r.isHidden(),
                        r.getCreatedAt()
                ));
    }

    /**
     * 리뷰 신고 처리 로직
     */
    @Transactional
    public void reportReview(Long reviewId, String username) {
        Member reporter = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 본인 리뷰는 신고 불가
        if (review.getMember().getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("본인이 작성한 리뷰는 신고할 수 없습니다.");
        }

        // 중복 신고 방지
        if (reviewReportRepository.findByReviewIdAndMemberId(reviewId, reporter.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 신고한 리뷰입니다.");
        }

        // 신고 내역 저장 (어뷰징 방지용)
        reviewReportRepository.save(ReviewReport.builder()
                .review(review)
                .member(reporter)
                .createdAt(LocalDateTime.now())
                .build());

        // 글로벌 신고 내역 저장 (관리자 대시보드용)
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.REVIEW)
                .targetId(review.getId())
                .reason("악성 리뷰 접수")
                .status(ReportStatus.PENDING)
                .build());

        // 신고 횟수 증가
        review.incrementReportCount();

        // 누적 3회 이상이면 숨김 처리
        if (review.getReportCount() >= 3) {
            review.hide();
            // 필요 시 관리자나 판매자에게 알림 발송 가능
        }
    }

    /**
     * 특정 판매자의 모든 리뷰 조회 로직
     */
    @Transactional(readOnly = true)
    public List<SellerReviewResponse> getReviewsBySeller(String username) {
        Member seller = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

        return reviewRepository.findByProductSellerId(seller.getId()).stream()
                .map(r -> new SellerReviewResponse(
                        r.getId(),
                        r.getProduct().getId(),
                        r.getProduct().getName(),
                        r.getRating(),
                        r.getContent(),
                        r.getMember().getNickname(),
                        r.getImageUrl(),
                        r.isHidden(),
                        r.getReportCount(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
