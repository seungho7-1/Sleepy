package com.sleepyproject.sleepy_backend.api;

import com.sleepyproject.sleepy_backend.api.dto.NotificationDto;
import com.sleepyproject.sleepy_backend.domain.notification.Notification;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getNotificationsForMember(username);
        
        List<NotificationDto> dtos = notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getUnreadNotificationsForMember(username);
        
        List<NotificationDto> dtos = notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        notificationService.markAsRead(id, username);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }
}
