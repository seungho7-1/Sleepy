package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostSyncScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final PostRepository postRepository;

    // cron = "초 분 시 일 월 요일" -> 5분마다 실행 (0분, 5분, 10분...)
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void syncViewCountsToDB() {
        log.info("==== Redis -> DB 동기화 스케줄러 시작 ====");
        // ==========================================
        // 1. 기존에 작성했던 [조회수] 동기화 로직
        // ==========================================
        ScanOptions viewOptions = ScanOptions.scanOptions().match("post:views:*").count(100).build();
        try (Cursor<byte[]> cursor = stringRedisTemplate.getConnectionFactory().getConnection().keyCommands().scan(viewOptions)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                Long postId = Long.parseLong(key.split(":")[2]);
                String countStr = stringRedisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    postRepository.addViewCount(postId, Integer.parseInt(countStr));
                    stringRedisTemplate.delete(key);
                }
            }
        } catch (Exception e) {
            log.error("조회수 동기화 중 에러 발생", e);
        }
        // ==========================================
        // 2. [새로 추가] 좋아요(Like) 수 동기화 로직
        // ==========================================
        ScanOptions likeOptions = ScanOptions.scanOptions().match("post:likes:*").count(100).build();
        try (Cursor<byte[]> cursor = stringRedisTemplate.getConnectionFactory().getConnection().keyCommands().scan(likeOptions)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                Long postId = Long.parseLong(key.split(":")[2]);

                // Redis Set의 크기(SCARD)가 곧 현재 좋아요 총개수!
                Long likeCountLong = stringRedisTemplate.opsForSet().size(key);
                int likeCount = likeCountLong != null ? likeCountLong.intValue() : 0;

                // DB에 최신 좋아요 개수 덮어쓰기
                postRepository.updateLikeCount(postId, likeCount);

                // 주의! 좋아요 키(Set)는 삭제하면 안 됩니다!
                // (누가 눌렀는지 상태를 계속 유지해야 하므로 delete 로직 없음)
            }
        } catch (Exception e) {
            log.error("좋아요 동기화 중 에러 발생", e);
        }
        log.info("==== Redis -> DB 동기화 완료 ====");
    }
}