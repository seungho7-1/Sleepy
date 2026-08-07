package com.sleepyproject.sleepy_backend.service.like;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.like.Likes;
import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.like.LikeRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeAsyncService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void syncLikeToDatabase(Member member, Long targetId, TargetType targetType, boolean isLiked) {

        // 1. DB에 이미 해당 유저가 이 타겟에 좋아요를 눌렀는지 실제 존재 여부 확인
        Optional<Likes> existing = likeRepository.findByMemberAndTargetIdAndTargetType(member, targetId, targetType);

        // 2. Redis가 알려준 isLiked가 true인데, DB에는 아직 데이터가 없다면 -> [좋아요 추가]
        if (isLiked && existing.isEmpty()) {
            likeRepository.save(Likes.builder()
                    .member(member)
                    .targetId(targetId)
                    .targetType(targetType)
                    .build());

            // 원자적 UPDATE 쿼리 사용 → Race Condition 방지
            if (targetType == TargetType.POST) {
                postRepository.incrementLikeCount(targetId);
            } else {
                reviewRepository.incrementLikeCount(targetId);
            }
        }
        // 3. Redis가 알려준 isLiked가 false인데, DB에는 데이터가 존재한다면 -> [좋아요 취소]
        else if (!isLiked && existing.isPresent()) {
            likeRepository.delete(existing.get());

            // 원자적 UPDATE 쿼리 사용 → Race Condition 방지
            if (targetType == TargetType.POST) {
                postRepository.decrementLikeCount(targetId);
            } else {
                reviewRepository.decrementLikeCount(targetId);
            }
        }
        // 그 외의 상태 (이미 처리되었거나 불일치하는 경우)는 불필요한 중복 연산을 막기 위해 무시합니다.
    }
}