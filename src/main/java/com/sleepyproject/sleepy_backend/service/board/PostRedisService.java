package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 조회수를 증가시킵니다.
     * 하루(24시간) 기준으로 중복을 방지합니다.
     */
    public void incrementViewCount(Long postId, String identifier) {
        // 1. 중복 방지용 키 생성 (예: view:post:1:user:testuser)
        String viewKey = "view:post:" + postId + ":user:" + identifier;
        // 2. setIfAbsent = 키가 없을 때만 데이터를 저장하고 true 반환 (TTL 24시간 설정)
        Boolean isFirstView = stringRedisTemplate.opsForValue().setIfAbsent(viewKey, "1", Duration.ofHours(24));

        // 3. 처음 조회한 사람이라면 (true) 조회수 카운터 1 증가
        if (Boolean.TRUE.equals(isFirstView)) {
            String countKey = "post:views:" + postId;
            stringRedisTemplate.opsForValue().increment(countKey);
        }
    }

    /**
     * Redis에서 현재 최신 조회수 증가분(또는 저장된 값)을 가져옵니다.
     * 값이 없으면 0을 반환합니다.
     */
    //실시간 조회수 가져오기(redis에 쌓인 임시 조회수)
    public int getCachedViewCount(Long postId) {
        String countKey = "post:views:" + postId;
        String val = stringRedisTemplate.opsForValue().get(countKey);
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * [좋아요 토글] Redis Set 활용
     * @return true면 좋아요 추가됨, false면 좋아요 취소됨
     */
    public boolean toggleLike(Long targetId, TargetType targetType, String username) {
        String likeKey = generateLikeKey(targetId, targetType);

        // 1. 해당 유저가 이미 Set에 들어있는지 확인 (SISMEMBER) -> O(1) 속도로 매우 빠름
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(likeKey, username);

        log.info("=== [Redis Toggle] key: {}, username: {}, isAlreadyMember: {} ===", likeKey, username, isMember);

        if (Boolean.TRUE.equals(isMember)) {
            // 2. 이미 눌렀다면 Set에서 유저 제거 (좋아요 취소)
            stringRedisTemplate.opsForSet().remove(likeKey, username);
            return false;
        } else {
            // 3. 안 눌렀다면 Set에 유저 추가 (좋아요 누름)
            stringRedisTemplate.opsForSet().add(likeKey, username);
            return true;
        }
    }
    // --- 헬퍼 메소드 ---
    private String generateLikeKey(Long targetId, TargetType targetType) {
        String typePrefix = targetType.name().toLowerCase();
        return typePrefix + ":likes:" + targetId;
    }

    /**
     * [좋아요 총개수] Redis Set의 크기 반환 (SCARD)
     */
    public int getCachedLikeCount(Long targetId, TargetType targetType) {
        String key = generateLikeKey(targetId, targetType);
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size == null ? 0 : size.intValue();
    }


    /**
     * [나의 좋아요 상태] 특정 유저가 타겟(게시글/리뷰 등)에 하트를 눌렀는지 확인
     */
    public boolean isLikedByUser(Long targetId, TargetType targetType, String username) {
        String likeKey = generateLikeKey(targetId, targetType);
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(likeKey, username));
    }

}
