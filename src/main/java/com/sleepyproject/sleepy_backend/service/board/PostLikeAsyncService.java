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

/**
 * DB I/O가 느려서 전체 응답 속도가 느려지는 것을 막기 위해,
 * 오직 DB에 좋아요 데이터를 썼다/지웠다 하는 작업만 별도의 쓰레드(비동기)에서 처리하게 만드는 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeAsyncService {

    private final PostLikeRepository postLikeRepository;

    @Async
    @Transactional
    public void syncLikeToDatabase(Member member, Post post, boolean isLiked) {
        try {
            Optional<PostLike> existingLike = postLikeRepository.findByMemberAndPost(member, post);
            
            // 만약 유저가 좋아요를 눌렀는데 DB엔 없다면 추가
            if (isLiked && existingLike.isEmpty()) {
                postLikeRepository.save(new PostLike(member, post));
            } 
            // 유저가 좋아요를 취소했는데 DB엔 여전히 있다면 삭제
            else if (!isLiked && existingLike.isPresent()) {
                postLikeRepository.delete(existingLike.get());
            }
            
            log.info("DB 비동기 좋아요 동기화 완료: postId={}, userId={}, isLiked={}", post.getId(), member.getId(), isLiked);
        } catch (Exception e) {
            log.error("DB 비동기 좋아요 동기화 중 에러 발생: postId={}, userId={}", post.getId(), member.getId(), e);
        }
    }
}
