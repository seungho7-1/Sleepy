package com.sleepyproject.sleepy_backend.service.notification;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.notification.Notification;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.repository.NotificationRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sleepyproject.sleepy_backend.service.member.MemberReader;
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
    private final MemberReader memberReader;
    private final MemberRepository memberRepository;

    @Value("${app.env:dev}")
    private String appEnv;

    @Value("${firebase.project-id:sleepy-frontend-eac65}")
    private String firebaseProjectId;

    @Value("${firebase.api-key:AIzaSyCko0AeT3hjwvGBlGydpJ-PjA445Txswxw}")
    private String firebaseApiKey;

    @Transactional
    public Notification createNotification(String username, NotificationType type, String message, String relatedUrl) {
        Member member = memberReader.getMember(username);
        Notification notification = Notification.builder()
                .member(member)
                .type(type)
                .message(message)
                .relatedUrl(relatedUrl)
                .build();
        Notification saved = notificationRepository.save(notification);
        java.util.concurrent.CompletableFuture.runAsync(() -> sendToFirebase(saved, member));
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
        java.util.concurrent.CompletableFuture.runAsync(() -> sendToFirebase(saved, member));
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
        Member member = memberReader.getMember(username);
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForMember(String username) {
        Member member = memberReader.getMember(username);
        return notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(member.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        Member member = memberReader.getMember(username);
        return notificationRepository.countByMemberIdAndIsReadFalse(member.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        if (!notification.getMember().getUsername().equals(username)) {
            throw new IllegalArgumentException("No permission");
        }
        notification.markAsRead();
        
        java.util.concurrent.CompletableFuture.runAsync(() -> markAsReadInFirebase(notificationId, username));
    }

    private void sendToFirebase(Notification notification, Member member) {
        try {
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String collection = "dev".equals(appEnv) ? "dev_notifications" : "notifications";
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s/%s/userNotifications/%d?key=%s", 
                firebaseProjectId, collection, encodedNickname, notification.getId(), firebaseApiKey);

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
                notification.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
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
            Member member = memberReader.getMember(username);
            if (member == null) return;
            
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String collection = "dev".equals(appEnv) ? "dev_notifications" : "notifications";
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/%s/%s/userNotifications/%d?updateMask.fieldPaths=isRead&key=%s", 
                firebaseProjectId, collection, encodedNickname, notificationId, firebaseApiKey);

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
        Member member = memberReader.getMember(username);
        
        List<Notification> unreadNotifications = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(member.getId());
        if (unreadNotifications.isEmpty()) return;
        
        // 벌크 업데이트로 한 번에 DB 처리
        notificationRepository.markAllAsReadByMemberId(member.getId());
        
        // 파이어베이스 동기화 비동기 병렬 처리
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            for (Notification notification : unreadNotifications) {
                markAsReadInFirebase(notification.getId(), username);
            }
        });
    }
}
