package com.sleepyproject.sleepy_backend.service.notification;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.notification.Notification;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.repository.NotificationRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Notification createNotification(String username, NotificationType type, String message, String relatedUrl) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Notification notification = Notification.builder()
                .member(member)
                .type(type)
                .message(message)
                .relatedUrl(relatedUrl)
                .build();
        return notificationRepository.save(notification);
    }
    
    @Transactional
    public Notification createNotificationByMember(Member member, NotificationType type, String message, String relatedUrl) {
        Notification notification = Notification.builder()
                .member(member)
                .type(type)
                .message(message)
                .relatedUrl(relatedUrl)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForMember(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForMember(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(member.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.countByMemberIdAndIsReadFalse(member.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid notification ID"));
        
        if (!notification.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("You don't have permission to modify this notification");
        }
        
        notification.markAsRead();
    }
}
