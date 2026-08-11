package com.sleepyproject.sleepy_backend.service.inquiry;

import com.sleepyproject.sleepy_backend.api.inquiry.dto.InquiryDto;
import com.sleepyproject.sleepy_backend.domain.inquiry.Inquiry;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.inquiry.InquiryRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.sleepyproject.sleepy_backend.service.member.MemberReader;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberReader memberReader;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Transactional
    public InquiryDto.Response createInquiry(String username, InquiryDto.Request request) {
        Member member = memberReader.getMember(username);


        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        
        // 관리자에게 알림 발송
        notificationService.notifyAllAdmins(
                NotificationType.NEW_INQUIRY, 
                "새로운 1:1 문의가 등록되었습니다: " + savedInquiry.getTitle(), 
                "/admin"
        );
        
        return InquiryDto.Response.fromEntity(savedInquiry);
    }

    // 해당 유저 문의목록 가져오기
    @Transactional(readOnly = true)
    public List<InquiryDto.Response> getMyInquiries(String username) {
        Member member = memberReader.getMember(username);

        return inquiryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .map(InquiryDto.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InquiryDto.Response> getAllInquiries() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InquiryDto.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void replyToInquiry(Long inquiryId, String replyContent) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        inquiry.addReply(replyContent);
        
        // 유저에게 알림 발송
        notificationService.createNotificationByMember(
                inquiry.getMember(), 
                NotificationType.INQUIRY_ANSWERED, 
                "작성하신 1:1 문의에 답변이 등록되었습니다.", 
                "/mypage?tab=inquiry"
        );
    }
}
