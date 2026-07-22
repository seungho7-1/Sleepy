package com.sleepyproject.sleepy_backend;

import com.sleepyproject.sleepy_backend.repository.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RepoTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    public void testRepo() {
        notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(1L);
        System.out.println("Repository works!");
    }
}
