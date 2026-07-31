package com.sleepyproject.sleepy_backend.domain.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@AllArgsConstructor
// 로그아웃 시 Access Token을 남은 수명만큼 저장하여 강제 무효화시킵니다. (TTL은 서비스 로직에서 동적으로 지정)
@RedisHash(value = "blacklistedToken")
public class BlackListedToken {

    @Id
    private String accessToken; // 로그아웃 처리된 Access Token 값

    private String email; // 누구의 토큰이었는지 기록용
}