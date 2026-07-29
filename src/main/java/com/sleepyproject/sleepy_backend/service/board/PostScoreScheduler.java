package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostScoreScheduler {

    private final PostRepository postRepository;

    /**
     * 매 시간마다 실행되어 최근 7일 동안 작성된 게시글들의 인기 점수(Popularity Score)를 갱신합니다.
     * 시간 감쇠 로직이 포함되어 있어, 오래된 글은 점수가 하락합니다.
     */
    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    @Transactional
    public void updatePostPopularityScores() {
        log.info("Starting scheduled task to update Post Popularity Scores...");
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Post> recentPosts = postRepository.findByCreatedAtAfter(sevenDaysAgo);
        
        for (Post post : recentPosts) {
            post.calculatePopularityScore();
        }
        
        log.info("Finished updating popularity scores for {} posts.", recentPosts.size());
    }
}
