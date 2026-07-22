package com.sleepyproject.sleepy_backend.service.admin;

import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.report.Report;
import com.sleepyproject.sleepy_backend.domain.report.ReportStatus;
import com.sleepyproject.sleepy_backend.domain.report.ReportTargetType;
import com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus;
import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.report.ReportRepository;
import com.sleepyproject.sleepy_backend.repository.seller.SellerApplicationRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long todaySignupCount = memberRepository.countByCreatedAtAfter(startOfToday);
        long todayProductCount = productRepository.countByCreatedAtAfter(startOfToday);
        long pendingSellerCount = sellerApplicationRepository.countByStatus(ApplicationStatus.PENDING);
        long newReportCount = reportRepository.countByStatus(ReportStatus.PENDING);

        return Map.of(
                "todaySignupCount", todaySignupCount,
                "todayProductCount", todayProductCount,
                "pendingSellerCount", pendingSellerCount,
                "newReportCount", newReportCount
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOldDashboardStats() {
        long totalMembers = memberRepository.count();
        long totalProducts = productRepository.count();

        List<Map<String, Object>> members = memberRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("email", m.getEmail());
                    map.put("nickname", m.getNickname());
                    map.put("role", m.getRole().name());
                    return map;
                })
                .collect(Collectors.toList());

        return Map.of(
                "totalMembers", totalMembers,
                "totalProducts", totalProducts,
                "members", members
        );
    }

    // Report
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING).stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("targetType", r.getTargetType().name());
                    map.put("targetId", r.getTargetId());
                    map.put("reason", r.getReason());
                    map.put("createdAt", r.getCreatedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void resolveReport(Long id, String action) {
        Report report = reportRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        if ("BLIND".equals(action) || "SUSPEND_USER".equals(action)) {
            // Blind the target
            if (report.getTargetType() == ReportTargetType.POST) {
                postRepository.findById(report.getTargetId()).ifPresent(Post::hide);
            } else if (report.getTargetType() == ReportTargetType.COMMENT) {
                commentRepository.findById(report.getTargetId()).ifPresent(Comment::hide);
            } else if (report.getTargetType() == ReportTargetType.PRODUCT) {
                productRepository.findById(report.getTargetId()).ifPresent(Product::hide);
            } else if (report.getTargetType() == ReportTargetType.REVIEW) {
                reviewRepository.findById(report.getTargetId()).ifPresent(Review::hide);
            }
            
            // Suspend the author if action is SUSPEND_USER
            if ("SUSPEND_USER".equals(action)) {
                Member author = getAuthorOfReportTarget(report);
                if (author != null) {
                    author.updateStatus("SUSPENDED");
                    notificationService.createNotificationByMember(
                            author,
                            NotificationType.SYSTEM_ALERT,
                            "신고 조치로 인해 계정이 정지되었습니다.",
                            "/my/profile"
                    );
                }
            }
        }
        
        report.resolve();
    }

    /**
     * 신고 접수 시 관리자 전체에게 알림 발송
     */
    public void notifyAdminsOfNewReport(String targetTypeName, Long targetId) {
        notificationService.notifyAllAdmins(
                NotificationType.NEW_REPORT,
                "새로운 신고가 접수되었습니다. (" + targetTypeName + " #" + targetId + ")",
                "/admin/reports"
        );
    }

    private Member getAuthorOfReportTarget(Report report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            return postRepository.findById(report.getTargetId()).map(Post::getMember).orElse(null);
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            return commentRepository.findById(report.getTargetId()).map(Comment::getMember).orElse(null);
        } else if (report.getTargetType() == ReportTargetType.PRODUCT) {
            return productRepository.findById(report.getTargetId()).map(Product::getSeller).orElse(null);
        } else if (report.getTargetType() == ReportTargetType.REVIEW) {
            return reviewRepository.findById(report.getTargetId()).map(Review::getMember).orElse(null);
        }
        return null;
    }

    // Members
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("email", m.getEmail());
                    map.put("nickname", m.getNickname());
                    map.put("role", m.getRole().name());
                    map.put("status", m.getStatus());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void suspendMember(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.updateStatus("SUSPENDED");
    }

    public void unsuspendMember(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.updateStatus("ACTIVE");
    }

    // Sellers
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingSellerApplications() {
        return sellerApplicationRepository.findByStatus(ApplicationStatus.PENDING).stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("shopName", s.getShopName());
                    map.put("siteUrl", s.getSiteUrl());
                    map.put("snsUrls", s.getSnsUrls());
                    map.put("introduction", s.getIntroduction());
                    map.put("memberId", s.getMember().getId());
                    map.put("memberNickname", s.getMember().getNickname());
                    map.put("memberEmail", s.getMember().getEmail());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void approveSellerApplication(Long id) {
        SellerApplication application = sellerApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller application not found"));
        
        application.updateStatus(ApplicationStatus.APPROVED);
        
        Member member = application.getMember();
        member.updateRole(Role.SELLER);
        member.updateSellerInfo(application.getSiteUrl(), application.getSnsUrls(), application.getShopName());

        // [신청자 알림] 판매자 신청 승인
        notificationService.createNotificationByMember(
                member,
                NotificationType.SELLER_APPROVAL,
                "축하합니다! 판매자 신청이 승인되었습니다.",
                "/my/seller-status"
        );
    }

    public void rejectSellerApplication(Long id, String reason) {
        SellerApplication application = sellerApplicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller application not found"));
        
        application.reject(reason);

        // [신청자 알림] 판매자 신청 반려
        String msg = "판매자 신청이 반려되었습니다.";
        if (reason != null && !reason.isBlank()) {
            msg += " 사유: " + reason;
        }
        notificationService.createNotificationByMember(
                application.getMember(),
                NotificationType.SELLER_REJECTED,
                msg,
                "/my/seller-status"
        );
    }

    // Products
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("shopName", p.getShopName());
                    map.put("price", p.getPrice());
                    map.put("imageUrl", p.getImageUrl());
                    map.put("isHidden", p.isHidden());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void hideProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.hide();
    }

    public void unhideProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.unhide();
    }
}
