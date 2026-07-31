package com.sleepyproject.sleepy_backend.domain.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// Redis에 "refreshToken"이라는 접두사로 저장되며, TTL(수명)은 14일(1209600초)로 설정합니다.
@RedisHash(value = "refreshToken", timeToLive = 1209600)
public class RefreshToken {

    @Id
    private String email; // 유저 이메일을 Key로 사용

    @Indexed
    private String refreshToken; // 발급된 리프레시 토큰 값

    // 동시성 이슈(예: React StrictMode, 빠른 연타)로 인한 RTR 탈취 오탐 방지를 위한 이전 토큰
    private String previousRefreshToken;

    public RefreshToken(String email, String refreshToken) {
        this.email = email;
        this.refreshToken = refreshToken;
        this.previousRefreshToken = null;
    }

}