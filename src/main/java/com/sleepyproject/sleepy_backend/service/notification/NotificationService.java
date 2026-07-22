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
        for (Member admin : admins) {
            createNotificationByMember(admin, type, message, relatedUrl);
        }
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
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString());
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/notifications/%s/userNotifications/%d?key=%s", 
                projectId, encodedNickname, notification.getId(), apiKey);
            java.net.URI uri = java.net.URI.create(urlStr);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-HTTP-Method-Override", "PATCH");

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            fields.put("id", Map.of("integerValue", notification.getId()));
            fields.put("type", Map.of("stringValue", notification.getType().name()));
            fields.put("message", Map.of("stringValue", notification.getMessage()));
            fields.put("relatedUrl", Map.of("stringValue", notification.getRelatedUrl() != null ? notification.getRelatedUrl() : ""));
            fields.put("isRead", Map.of("booleanValue", notification.isRead()));
            fields.put("createdAt", Map.of("integerValue", notification.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000));
            body.put("fields", fields);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
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
            String encodedNickname = URLEncoder.encode(member.getNickname(), StandardCharsets.UTF_8.toString());
            String urlStr = String.format("https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents/notifications/%s/userNotifications/%d?updateMask.fieldPaths=isRead&key=%s", 
                projectId, encodedNickname, notificationId, apiKey);
            java.net.URI uri = java.net.URI.create(urlStr);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-HTTP-Method-Override", "PATCH");

            Map<String, Object> body = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();
            fields.put("isRead", Map.of("booleanValue", true));
            body.put("fields", fields);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Firebase update failed: " + e.getStatusCode());
            System.err.println("Response body: " + e.getResponseBodyAsString());
            e.printStackTrace();
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
