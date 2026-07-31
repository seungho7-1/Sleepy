package com.sleepyproject.sleepy_backend.repository.redis;

import com.sleepyproject.sleepy_backend.domain.redis.BlackListedToken;
import org.springframework.data.repository.CrudRepository;

public interface BlackListedTokenRepository extends CrudRepository<BlackListedToken, String> {
}
