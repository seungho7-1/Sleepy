package com.sleepyproject.sleepy_backend.repository;

import com.sleepyproject.sleepy_backend.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<Notification> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(Long memberId);
    long countByMemberIdAndIsReadFalse(Long memberId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Notification n WHERE n.member.id = :memberId")
    void deleteByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
