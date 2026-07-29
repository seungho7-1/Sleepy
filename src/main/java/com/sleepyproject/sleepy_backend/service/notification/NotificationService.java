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
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    @Value("${app.env:dev}")
    private String appEnv;

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
        Notification saved = notificationRepository.save(notification);
        sendToFirebase(saved, member);
        return saved;
    }
    
    @Transactional
    public Notification createNotificationByMember(Member member, NotificationType type, String message, String relatedUrl) {
        Notification notification = Notification.builder()
                .member(member)
                .type(type)
                .message(message)
                .relatedUrl(relatedUrl)
                .build();
        Notification saved = notificationRepository.save(notification);
        sendToFirebase(saved, member);
        return saved;
    }

    /**
     * 모든 관리자(ADMIN)에게 동일한 알림을 발송합니다.
     */
    @Transactional
    public void notifyAllAdmins(NotificationType type, String message, String relatedUrl) {
        List<Member> admins = memberRepository.findByRole(com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN);
        
        // 중복 계정/닉네임으로 인한 중복 알림 방지 (닉네임 기준 필터링)
        java.util.Set<String> notifiedNicknames = new java.util.HashSet<>();
        for (Member admin : admins) {
            if (!notifiedNicknames.contains(admin.getNickname())) {
                createNotificationByMember(admin, type, message, relatedUrl);
                notifiedNicknames.add(admin.getNickname());
            }
        }
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Notification> getNotificationsForMember(String username, org.springframework.data.domain.Pageable pageable) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId(), pageable);
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
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        
        if (notification != null) {
            if (!notification.getMember().getUsername().equals(username)) {
                throw new IllegalArgumentException("No permission");
            }
            notification.markAsRead();
        }
        
        markAsReadInFirebase(notificationId, username);
    }

    private void sendToFirebase(Notification notification, Member member) {
        try {
            String projectId = "sleepy-frontend-eac65";
            String apiKey = "AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw";
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String collection = "dev".equals(appEnv) ? "dev_notifications" : "notifications";
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s/%s/userNotifications/%d?key=%s", 
                projectId, collection, encodedNickname, notification.getId(), apiKey);

            String jsonBody = String.format(
                "{\"fields\": {" +
                "\"id\": {\"integerValue\": \"%d\"}," +
                "\"type\": {\"stringValue\": \"%s\"}," +
                "\"message\": {\"stringValue\": \"%s\"}," +
                "\"relatedUrl\": {\"stringValue\": \"%s\"}," +
                "\"isRead\": {\"booleanValue\": %b}," +
                "\"createdAt\": {\"integerValue\": \"%d\"}" +
                "}}",
                notification.getId(),
                notification.getType().name(),
                notification.getMessage().replace("\"", "\\\""),
                notification.getRelatedUrl() != null ? notification.getRelatedUrl() : "",
                notification.isRead(),
                notification.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(urlStr))
                    .header("Content-Type", "application/json")
                    .header("X-HTTP-Method-Override", "PATCH")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markAsReadInFirebase(Long notificationId, String username) {
        try {
            Member member = memberRepository.findByUsername(username).orElse(null);
            if (member == null) return;
            
            String projectId = "sleepy-frontend-eac65";
            String apiKey = "AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw";
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String collection = "dev".equals(appEnv) ? "dev_notifications" : "notifications";
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s/%s/userNotifications/%d?updateMask.fieldPaths=isRead&key=%s", 
                projectId, collection, encodedNickname, notificationId, apiKey);

            String jsonBody = "{\"fields\": {\"isRead\": {\"booleanValue\": true}}}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(urlStr))
                    .header("Content-Type", "application/json")
                    .header("X-HTTP-Method-Override", "PATCH")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void markAllAsRead(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<Notification> unreadNotifications = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(member.getId());
        
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
            markAsReadInFirebase(notification.getId(), username);
        }
    }
}
