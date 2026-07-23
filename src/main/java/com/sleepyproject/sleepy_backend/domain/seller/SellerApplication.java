package com.sleepyproject.sleepy_backend.domain.seller;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "seller_application")
public class SellerApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String siteUrl;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    private String shopName;

    @Column(length = 20)
    private String businessNumber;

    @Column(length = 1000)
    private String snsUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    public void updateStatus(ApplicationStatus status) {
        this.status = status;
        if (status != ApplicationStatus.REJECTED) {
            this.rejectionReason = null;
        }
    }

    public void reject(String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
