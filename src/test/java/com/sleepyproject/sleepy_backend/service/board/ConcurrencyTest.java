package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class ConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyTest.class);

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("동시성 문제(Lost Update) 증명 테스트 - 100명이 동시에 조회수 증가")
    public void lostUpdateTest() throws InterruptedException {
        // 1. 기존에 있는 게시글 중 하나를 가져옵니다. (로컬 테스트용이므로 데이터가 하나라도 있어야 함)
        List<Post> posts = postRepository.findAll();
        if (posts.isEmpty()) {
            log.warn("테스트를 진행할 게시글 데이터가 없습니다. 프론트엔드에서 글을 하나 작성해주세요.");
            return;
        }
        
        final Long postId = posts.get(0).getId();
        int initialViews = posts.get(0).getViewCount();
        log.info("====== [테스트 시작] 선택된 게시글 ID: {}, 초기 조회수: {} ======", postId, initialViews);

        // 2. 100명의 유저(Thread) 준비
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        // 3. 100명이 동시에 똑같은 게시글을 조회해서 조회수를 +1 하는 로직 (Redis 미적용 구버전 방식)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // DB에서 통째로 게시글 조회
                    Post post = postRepository.findById(postId).orElseThrow();
                    
                    // 더티 체킹을 통한 값 증가 (AS-IS 방식)
                    post.incrementViewCount(); // 기존에 구현되어 있던 메서드 활용
                    postRepository.save(post);
                    
                } catch (Exception e) {
                    log.error("에러 발생: ", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 100개의 요청이 모두 끝날 때까지 대기
        latch.await();
        long endTime = System.currentTimeMillis();

        // 4. 최종 결과 확인
        Post finalPost = postRepository.findById(postId).orElseThrow();
        log.info("====== [테스트 종료] ======");
        log.info("요청된 총 유저 수: {}", threadCount);
        log.info("실제 DB에 반영된 최종 조회수 증가량: {}", (finalPost.getViewCount() - initialViews));
        log.info("유실된 조회수(Lost Update): {}", (threadCount - (finalPost.getViewCount() - initialViews)));
        log.info("총 소요 시간: {} ms", (endTime - startTime));
        
        // 데이터는 삭제하지 않고 유지합니다. (실제 데이터일 수 있으므로)
    }


}
