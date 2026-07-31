package com.sleepyproject.sleepy_backend.repository.redis;

import com.sleepyproject.sleepy_backend.domain.redis.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
