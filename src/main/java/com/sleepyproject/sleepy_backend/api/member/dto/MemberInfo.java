package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 마이페이지 회원 정보 응답 DTO
 * 현재 로그인한 유저의 프로필 정보를 담아 클라이언트로 반환합니다.
 */
@Getter
@AllArgsConstructor
public class MemberInfo {

    // 회원 고유 ID
    private Long id;

    private String username;

    // 이메일 주소
    private String email;

    // 닉네임 (마이페이지에서 수정 가능)
    private String nickname;

    // 역할 (BUYER 또는 SELLER)
    private String role;

    // 가입 일시 (문자열 포맷으로 전달)
    private String createdAt;

    // OAuth 제공자 (KAKAO, NAVER 등, 없을 경우 null)
    private String oauthProvider;

    // 프로필 이미지 URL
    private String profileImageUrl;
}