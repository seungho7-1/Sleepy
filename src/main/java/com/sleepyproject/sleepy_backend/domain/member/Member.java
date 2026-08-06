package com.sleepyproject.sleepy_backend.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

/**
 * 회원(사용자) 정보를 정의하는 도메인 엔티티 클래스입니다.
 * - 로그인 정보(이메일, 비밀번호), 프로필 정보(닉네임, 역할), 가입일 기록
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime createdAt;

    private String oauthProvider; // KAKAO, NAVER

    private String oauthAccessToken;

    private String oauthRefreshToken;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean onboarded = false;

    // 프로필 이미지 URL
    private String profileImageUrl;

    // 스토어명 (판매자 필수)
    private String shopName;

    // 쇼핑몰 사이트 URL (판매자 전용)
    private String siteUrl;

    // 소개글 (판매자 전용)
    private String introduction;

    // SNS 링크 (판매자 전용)
    private String youtubeUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String tiktokUrl;

    // 계정 상태 (ACTIVE, SUSPENDED)
    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    // 최근 접속일
    private LocalDateTime lastLoginAt;

    // 쇼핑몰 주소 (판매자 필수)
    private String shopUrl;

    // SNS 주소 목록 (선택, 콤마(,) 구분)
    @Column(length = 1000)
    private String snsUrls;

    public void completeOnboarding() {
        this.onboarded = true;
    }

    public void updateSellerInfo(String shopUrl, String snsUrls, String shopName) {
        this.shopUrl = shopUrl;
        this.snsUrls = snsUrls;
        this.shopName = shopName;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 닉네임 수정 메서드 (더티 체킹 방식으로 트랜잭션 내에서 자동 반영됨)
     *
     * @param nickname 변경할 새 닉네임
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateOauthTokens(String accessToken, String refreshToken) {
        this.oauthAccessToken = accessToken;
        this.oauthRefreshToken = refreshToken;
    }

    public void updateOauthProvider(String provider) {
        this.oauthProvider = provider;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateSellerProfile(String shopName, String siteUrl, String introduction, String youtubeUrl, String instagramUrl, String facebookUrl, String tiktokUrl) {
        if (shopName != null) this.shopName = shopName;
        if (siteUrl != null) this.siteUrl = siteUrl;
        this.introduction = introduction;
        this.youtubeUrl = youtubeUrl;
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.tiktokUrl = tiktokUrl;
    }

    public void updateSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }
}