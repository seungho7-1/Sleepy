package com.sleepyproject.sleepy_backend.service.seller;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus;
import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.seller.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private final SellerApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;

    //판매자 등록
    @Transactional
    public void submitApplication(String username, String siteUrl, String introduction) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SellerApplication application = SellerApplication.builder()
                .member(member)
                .siteUrl(siteUrl)
                .introduction(introduction)
                .status(ApplicationStatus.PENDING)
                .build();
        applicationRepository.save(application);
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
