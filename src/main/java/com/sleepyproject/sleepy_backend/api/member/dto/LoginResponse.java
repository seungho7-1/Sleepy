package com.sleepyproject.sleepy_backend.api.member.dto;

import com.sleepyproject.sleepy_backend.domain.member.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder // 빌더 패턴을 쓰면 객체 생성이 아주 편합니다.
public class LoginResponse {
    private Long memberId;   // 로그인한 회원의 고유 ID (프론트에서 상품 소유권 판별에 사용)
    private String accessToken;
    private String username;
    private String email;
    private String nickname;
    private Role role;
}