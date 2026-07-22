package com.sleepyproject.sleepy_backend;

import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.lang.reflect.Method;

@SpringBootTest
public class FirebaseTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void testMarkAsRead() throws Exception {
        Method method = NotificationService.class.getDeclaredMethod("markAsReadInFirebase", Long.class, String.class);
        method.setAccessible(true);
        // Using a fake notification ID that might not exist, but let's just see what Firebase returns
        method.invoke(notificationService, 9999L, "testuser");
    }
}
