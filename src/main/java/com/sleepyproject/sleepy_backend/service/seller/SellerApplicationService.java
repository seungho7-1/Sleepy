package com.sleepyproject.sleepy_backend.service.seller;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus;
import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.seller.SellerApplicationRepository;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private final SellerApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    /**
     * 판매자 신청 등록.
     * 신청 저장 후 모든 관리자에게 NEW_SELLER_APPLICATION 알림을 발송합니다.
     */
    @Transactional
    public void submitApplication(String username, String siteUrl, String introduction, String shopName, String snsUrls) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check if there is already a pending application for this member
        boolean hasPending = applicationRepository.findAll().stream()
                .anyMatch(app -> app.getMember().getId().equals(member.getId()) 
                              && app.getStatus() == ApplicationStatus.PENDING);
        if (hasPending) {
            throw new IllegalArgumentException("이미 대기 중인 판매자 심사 요청이 있습니다.");
        }

        SellerApplication application = SellerApplication.builder()
                .member(member)
                .siteUrl(siteUrl)
                .introduction(introduction)
                .shopName(shopName)
                .snsUrls(snsUrls)
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(application);

        // [관리자 알림] 새 판매자 신청이 접수되면 모든 관리자에게 알림
        notificationService.notifyAllAdmins(
                NotificationType.NEW_SELLER_APPLICATION,
                "새로운 판매자 신청이 접수되었습니다. (" + shopName + ")",
                "/admin/applications"
        );
    }

    @Transactional(readOnly = true)
    public List<SellerApplication> getPendingApplications() {
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.PENDING)
                .toList();
    }

    @Transactional
    public void approveApplication(Long applicationId) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.updateStatus(ApplicationStatus.APPROVED);
        application.getMember().updateRole(Role.SELLER);
        application.getMember().updateSellerInfo(application.getSiteUrl(), application.getSnsUrls(), application.getShopName());
    }

    @Transactional
    public void rejectApplication(Long applicationId, String reason) {
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.reject(reason);
    }

    @Transactional(readOnly = true)
    public SellerApplication getLatestApplication(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        List<SellerApplication> apps = applicationRepository.findAll().stream()
                .filter(app -> app.getMember().getId().equals(member.getId()))
                .toList();
        
        if (apps.isEmpty()) {
            return null;
        }
        // Return the latest application by ID (assuming auto-increment ID represents timeline)
        return apps.get(apps.size() - 1);
    }
}
