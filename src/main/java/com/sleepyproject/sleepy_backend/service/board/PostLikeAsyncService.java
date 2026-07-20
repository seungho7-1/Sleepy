package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.board.PostLike;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.board.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeAsyncService {

    private final PostLikeRepository postLikeRepository;

    /**
     * 비동기로 좋아요 상태를 DB에 동기화합니다.
     * @Async 어노테이션이 작동하려면 반드시 별도의 클래스(Bean)로 분리해서 호출해야 합니다.
     * (같은 클래스 안에서 호출하면 프록시를 타지 않아 비동기로 동작하지 않음)
     */
    @Async
    @Transactional
    public void syncLikeToDatabase(Member member, Post post, boolean isLiked) {
        log.info("[Async] 백그라운드 DB 동기화 시작 - 게시글 ID: {}, 유저: {}, 좋아요 상태: {}", 
                 post.getId(), member.getUsername(), isLiked);
                 
        try {
            Optional<PostLike> existingLike = postLikeRepository.findByMemberAndPost(member, post);

            if (isLiked && existingLike.isEmpty()) {
                postLikeRepository.save(new PostLike(member, post));
            } else if (!isLiked && existingLike.isPresent()) {
                postLikeRepository.delete(existingLike.get());
            }
            
            log.info("[Async] 백그라운드 DB 동기화 완료!");
        } catch (Exception e) {
            log.error("[Async] DB 동기화 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}
